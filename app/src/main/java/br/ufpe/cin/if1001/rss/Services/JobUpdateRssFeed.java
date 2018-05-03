package br.ufpe.cin.if1001.rss.Services;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

import br.ufpe.cin.if1001.rss.R;
import br.ufpe.cin.if1001.rss.db.SQLiteRSSHelper;
import br.ufpe.cin.if1001.rss.domain.ItemRSS;
import br.ufpe.cin.if1001.rss.ui.MainActivity;
import br.ufpe.cin.if1001.rss.util.Constants;
import br.ufpe.cin.if1001.rss.util.ParserRSS;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class JobUpdateRssFeed extends JobService {
    private static final String TAG = "JOB_SERVICE";
    private SQLiteRSSHelper db;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.i(TAG, "onStartJob");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String linkFeed = preferences.getString(getString(R.string.rss_feed_key_name),
                getResources().getString(R.string.rss_feed_link));

        Intent loadServiceIntent = new Intent(getApplicationContext(), UpdateRssFeedDataBaseService.class);
        loadServiceIntent.putExtra(Constants.RSS_FEED_KEY_NAME, linkFeed);
        startService(loadServiceIntent);
        jobFinished(jobParameters, false);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.i(TAG, "onStopJob");
        return false;
    }
}
