package joe.amrhein.drawsvgdemo;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import joe.amrhein.drawsvgdemo.svg.SVGSerializer;
import joe.amrhein.drawsvgdemo.utils.FloatingPoint;
import joe.amrhein.drawsvgdemo.views.PathTrackingView;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    PathTrackingView pathTrackingView;
    private int canvasWidth;
    private int canvasHeight;
    private String fileAuthority;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        pathTrackingView = (PathTrackingView) findViewById(R.id.canvas);
        //TODO possible reload if screen rotates
        canvasWidth = pathTrackingView.getWidth();
        canvasHeight = pathTrackingView.getHeight();

        fileAuthority = getString(R.string.file_authority);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        fab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                pathTrackingView.removeLastPath();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                if (pathTrackingView.isEmpty()) {
                    Toast.makeText(this, R.string.canvas_empty_message, Toast.LENGTH_SHORT).show();
                } else {
                    new SaveCanvasToSVGTask().execute(pathTrackingView.getSvgPaths());
                }
                return true;

            case R.id.action_clear:
                pathTrackingView.clear();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void sendFileToEmailIntent(File svgFile) {
        if (svgFile != null) {

            ArrayList<Parcelable> attachment = new ArrayList<>(1);
            attachment.add(FileProvider.getUriForFile(this, fileAuthority, svgFile));

            Intent i = new Intent(Intent.ACTION_SEND_MULTIPLE);
            i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachment);
            i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            i.setType("application/octet-stream");
            startActivity(Intent.createChooser(i, "Send To"));
        } else {
            Toast.makeText(this, "Error generating SVG", Toast.LENGTH_SHORT).show();
        }
    }


    public class SaveCanvasToSVGTask
            extends AsyncTask<LinkedList<List<FloatingPoint>>, Void, File> {

        @Override
        protected File doInBackground(LinkedList<List<FloatingPoint>>... params) {
            if (params.length < 1) {
                return null;
            }

            String svgContents = SVGSerializer.serializeToSVG(canvasWidth, canvasHeight, params[0]);
            Log.d(TAG, svgContents);

            if (svgContents == null) {
                return null;
            }

            BufferedWriter bw = null;
            try {
                File f = new File(getApplicationContext().getFilesDir(),
                        "canvas_" + System.currentTimeMillis() + ".svg");
                bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)));
                bw.write(svgContents);

                return f;
            } catch (IOException e) {
                Log.e(TAG, "Error saving SVG to disk", e);
            } finally {
                if (bw != null) {
                    try {
                        bw.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing output stream");
                    }
                }
            }
            return null;
        }


        @Override
        protected void onPostExecute(File svgFile) {
            sendFileToEmailIntent(svgFile);
        }
    }
}
