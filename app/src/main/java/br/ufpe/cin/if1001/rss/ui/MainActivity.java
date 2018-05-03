package br.ufpe.cin.if1001.rss.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import br.ufpe.cin.if1001.rss.R;
import br.ufpe.cin.if1001.rss.Services.JobUpdateRssFeed;
import br.ufpe.cin.if1001.rss.Services.UpdateRssFeedDataBaseService;
import br.ufpe.cin.if1001.rss.db.SQLiteRSSHelper;
import br.ufpe.cin.if1001.rss.util.Constants;

public class MainActivity extends Activity {

    private ListView conteudoRSS;
    private RssFeedReceiver receiver;
    private SQLiteRSSHelper db;
    private final String RSS_FEED = "http://rss.cnn.com/rss/edition.rss";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = SQLiteRSSHelper.getInstance(this);

        conteudoRSS = findViewById(R.id.conteudoRSS);

        SimpleCursorAdapter adapter =
            new SimpleCursorAdapter(
                //contexto, como estamos acostumados
                this,
                //Layout XML de como se parecem os itens da lista
                R.layout.item,
                //Objeto do tipo Cursor, com os dados retornados do banco.
                //Como ainda não fizemos nenhuma consulta, está nulo.
                null,
                //Mapeamento das colunas nos IDs do XML.
                // Os dois arrays a seguir devem ter o mesmo tamanho
                new String[]{SQLiteRSSHelper.ITEM_TITLE, SQLiteRSSHelper.ITEM_DATE},
                new int[]{R.id.itemTitulo, R.id.itemData},
                //Flags para determinar comportamento do adapter, pode deixar 0.
                0
            );
        //Seta o adapter. Como o Cursor é null, ainda não aparece nada na tela.
        conteudoRSS.setAdapter(adapter);

        // permite filtrar conteudo pelo teclado virtual
        conteudoRSS.setTextFilterEnabled(true);

        //Complete a implementação deste método de forma que ao clicar, o link seja aberto no navegador e
        // a notícia seja marcada como lida no banco
        conteudoRSS.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SimpleCursorAdapter adapter = (SimpleCursorAdapter) parent.getAdapter();
                Cursor mCursor = ((Cursor) adapter.getItem(position));
                String link = mCursor.getString(mCursor.getColumnIndexOrThrow(SQLiteRSSHelper.ITEM_LINK));
                db.markAsRead(link);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                browserIntent.setData(Uri.parse(link));
                startActivity(browserIntent);
            }
        });

        IntentFilter filter = new IntentFilter(RssFeedReceiver.GET_ITEMS_FINISHED);
        receiver = new RssFeedReceiver();
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        //Obtendo das preferências o link a ser utilizado como fonte das notícias.
        String linkfeed = preferences.getString(getString(R.string.rss_feed_key_name),
                getResources().getString(R.string.rss_feed_link));

        if(!IsJobRunning(Integer.parseInt(getString(R.string.job_update_feed_id))))
            ScheduleJob();

        //Criando o intent que chamará o serviço de atualizar o banco de dados
        Intent loadServiceIntent = new Intent(getApplicationContext(), UpdateRssFeedDataBaseService.class);
        loadServiceIntent.putExtra(Constants.RSS_FEED_KEY_NAME, linkfeed);
        startService(loadServiceIntent);
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mainmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btn_Config:
                startActivity(new Intent(this, ConfigActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public boolean IsJobRunning (int jobId) {
        JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);

        for ( JobInfo jobInfo : scheduler.getAllPendingJobs()) {
            Log.i("JOB_COUNT", Integer.toString(jobInfo.getId()));
            if ( jobInfo.getId() == jobId )
                return true;
        }

        return false;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void ScheduleJob(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int frequency = Integer.parseInt(preferences.getString(
                                getString(R.string.rss_update_frequency_key_name),
                                getString(R.string.default_rss_update_frequency)));

        ComponentName cn = new ComponentName(this, JobUpdateRssFeed.class);
        int jobId = Integer.parseInt(getString(R.string.job_update_feed_id));
        JobInfo ji = new JobInfo.Builder(jobId, cn)
                .setPeriodic(3000)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build();

        JobScheduler js = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        js.schedule(ji);
        Log.i("JOB_SCHEDULER", "Job scheduled!");
    }

    public class RssFeedReceiver extends BroadcastReceiver {
        public static final String GET_ITEMS_FINISHED = "GET_ITEMS_FINISHED";

        @Override
        public void onReceive(Context context, Intent intent) {
            //Carregando as notícias na tela.
            //Método acionado ao usar "sendBroadcast" no serviço UpdateRssFeedDataBaseService.
            new ExibirFeed().execute();
        }
    }

    public class ExibirFeed extends AsyncTask<Void, Void, Cursor> {

        @Override
        protected Cursor doInBackground(Void... voids) {
            Cursor c = db.getItems();
            c.getCount();
            return c;
        }

        @Override
        protected void onPostExecute(Cursor c) {
            if (c != null) {
                ((CursorAdapter) conteudoRSS.getAdapter()).changeCursor(c);
            }
        }
    }
}
