package com.andrealazzarotto.grappaapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class MainActivity extends AppCompatActivity {
    // URL del feed che ci interessa
    private static final String RSS_URL = "http://grappalug.org/feed/";
    // etichette usate per salvare e ripristinare l'instanceState
    private static final String ARTICLE_TITLE = "article_title";
    private static final String ARTICLE_URL = "article_url";

    // oggetti dell'interfaccia
    private View content;
    private View progress;
    private FloatingActionButton fab;
    private TextView title;
    private TextView url;

    // altre variabili usate a runtime
    private String articleTitle;
    private String articleURL;
    private AsyncTask<Void, Void, Boolean> task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        content = findViewById(R.id.include);
        progress = findViewById(R.id.progressBar);
        title = (TextView) findViewById(R.id.title_view);
        url = (TextView) findViewById(R.id.url_view);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Verifica se sta già ricaricando
                if (task == null)
                    loadFeed();
            }
        });

        // Verifica se l'interfaccia era stata salvata o se si parte da zero
        if (savedInstanceState != null) {
            articleTitle = savedInstanceState.getString(ARTICLE_TITLE);
            articleURL = savedInstanceState.getString(ARTICLE_URL);
            if (articleURL != null)
                displayTheData();
            else
                loadFeed();
        }
        else
            loadFeed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARTICLE_TITLE, articleTitle);
        outState.putString(ARTICLE_URL, articleURL);
    }

    private void showLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        content.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void displayTheData() {
        showLoading(false);
        title.setText(articleTitle);
        url.setText(articleURL);
    }

    private void loadFeed() {
        showLoading(true);
        task = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                // Prendi il contenuto del feed RSS
                StringBuilder xml = new StringBuilder();
                try {
                    URLConnection connection = (new URL(RSS_URL)).openConnection();
                    connection.connect();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    for (String line; (line = reader.readLine()) != null; )
                        xml.append(line);
                } catch (IOException e) {
                    return false;
                }

                /*
                 * Estrazione del titolo e del link di un elemento a caso
                 *
                 * Questo codice è fatto un po' "alla buona". Normalmente andrebbe usato un parser
                 * XML per leggere in modo robusto gli elementi del feed.
                 */
                String[] items = xml.toString().split("<.?item>\\s*");
                ArrayList<String> articles = new ArrayList<>();
                for (String item : items)
                    if (item.startsWith("<title"))
                        articles.add(item);

                int amount = articles.size();
                if (amount < 1)
                    return false;

                int which = (new Random()).nextInt(amount);
                String item = articles.get(which);
                articleTitle = item.split("<.?title>")[1];
                articleURL = item.split("<.?link>")[1];

                return true;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                task = null;
                displayTheData();
                if (!success)
                    Snackbar.make(fab, R.string.network_error, Snackbar.LENGTH_LONG).show();
            }
        };
        task.execute();
    }
}
