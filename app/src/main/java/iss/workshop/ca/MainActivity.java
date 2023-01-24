package iss.workshop.ca;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.splashscreen.SplashScreen;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
//https://stocksnap.io
public class MainActivity extends AppCompatActivity{
    private RecyclerAdaptor recyclerAdaptor;
    private String[] images = new String[20];
    public static boolean[] selected;
    private int clicks = 0, serviceId = 0;
    public static int imgCount, imgSelectedCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selected = new boolean[20];
        imgCount = 0;
        imgSelectedCount = 0;

        clearFolder();
        registerReceivers();
        setFetchBtn();
        setRecycler();
    }

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            TextView infotext = findViewById(R.id.infotext);
            ProgressBar progressBar = findViewById(R.id.progressBar);
            int id = intent.getIntExtra(Constants.serviceId, 0);

            if(action == null){
                return;
            }

            if(serviceId == id){
                if(action.equals(Constants.invalidUrl)){
                    infotext.setText("Invalid URL, please try again.");
                    progressBar.setVisibility(View.GONE);
                }

                if(action.equals(Constants.dwnComplete)){
                    if(imgCount > 0){
                        if(imgCount == 20){
                            AppCompatButton btn = findViewById(R.id.fetchbtn);
                            btn.setEnabled(false);
                            infotext.setText("To proceed, please select 6 images.");
                            progressBar.setVisibility(View.GONE);
                        }
                        else{
                            infotext.setText("Insufficient images, please try another URL.");
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                    else if(imgCount == 0){
                        infotext.setText("Insufficient images, please try another URL.");
                        progressBar.setVisibility(View.GONE);
                    }
                }

                if(action.equals(Constants.setImage)){
                    if(imgCount == Constants.imgMax){
                        AppCompatButton btn = findViewById(R.id.fetchbtn);
                        btn.setEnabled(false);
                    }

                    if(imgCount < Constants.imgMax){
                        String imgPath = intent.getStringExtra(Constants.imgPath);
                        setImage(imgPath);
                        imgCount++;

                        infotext.setText("Downloading... (" + imgCount + "/" + Constants.imgMax + ")");
                        progressBar.setProgress(imgCount);
                    }
                }
            }

            if(action.equals(Constants.setSelected)){
                int pos = intent.getIntExtra(Constants.pos, -1);

                if(pos == -1){
                    return;
                }

                selected[pos] = true;
                if(imgSelectedCount == 6){
                    startGame();
                }
            }

            if(action.equals(Constants.setUnselected)){
                int pos = intent.getIntExtra(Constants.pos, -1);

                if(pos == -1){
                    return;
                }

                selected[pos] = false;
            }
        }
    };

    protected void registerReceivers(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.dwnComplete);
        filter.addAction(Constants.invalidUrl);
        filter.addAction(Constants.setImage);
        filter.addAction(Constants.setSelected);
        filter.addAction(Constants.setUnselected);

        registerReceiver(receiver, filter);
    }

    protected void setFetchBtn(){
        TextView infotext = findViewById(R.id.infotext);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        AppCompatButton btn = findViewById(R.id.fetchbtn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clicks++;

                if(clicks > 1){
                    Intent stopIntent = new Intent(MainActivity.this, DownloadService.class);
                    stopService(stopIntent);
                    if(imgCount < Constants.imgMax && imgCount != 0){
                        Toast.makeText(MainActivity.this, "Download interrupted.", Toast.LENGTH_SHORT).show();
                    }

                    for(int i = 0; i<Constants.imgMax; i++){
                        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                        File dImg = new File(dir, "defaultimg.png");
                        recyclerAdaptor.setImage(i, dImg.getAbsolutePath());
                    }

                    imgCount = 0;
                }

                if(imgCount == 0){
                    infotext.setText("Downloading... (" + imgCount + "/" + Constants.imgMax + ")");
                    progressBar.setMax(Constants.imgMax);
                    progressBar.setProgress(0);
                    progressBar.setVisibility(View.VISIBLE);
                }

                startDownload();
            }
        });
    }

    protected void setRecycler(){
        RecyclerView recyclerView = findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(MainActivity.this, Constants.mainCol));

        String dImgPath = saveDefaultImg();

        Arrays.fill(images, dImgPath);
        Arrays.fill(selected, false);
        recyclerAdaptor = new RecyclerAdaptor(images);
        recyclerView.setAdapter(recyclerAdaptor);
    }

    protected String saveDefaultImg(){
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.raw.defaultimg);
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File dImg = new File(dir, "defaultimg.png");

        try {
            FileOutputStream out = new FileOutputStream(dImg);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return dImg.getAbsolutePath();
    }

    protected void clearFolder(){
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if(dir.isDirectory() && dir.exists()){
            for(File file: dir.listFiles()){
                file.delete();
            }
        }
    }

    protected void startDownload(){
        Intent downloadIntent = new Intent(MainActivity.this, DownloadService.class);
        downloadIntent.setAction(Constants.action_download);

        EditText inputbox = findViewById(R.id.inputbox);
        String url = inputbox.getText().toString();
        //url = "https://stocksnap.io";

        serviceId++;
        downloadIntent.putExtra(Constants.url, url);
        downloadIntent.putExtra(Constants.serviceId, serviceId);
        startService(downloadIntent);
    }

    protected void setImage(String imgPath){
        recyclerAdaptor.setImage(imgCount, imgPath);
    }

    public void startGame(){
        Intent gameIntent = new Intent(MainActivity.this, GameActivity.class);
        String[] selectedImgs = new String[Constants.imgSelectedMax];
        int count = 0;

        for(int i = 0; i < Constants.imgMax; i++){
            if(selected[i] == true){
                selectedImgs[count] = images[i];
                count++;
            }

            if(count == Constants.imgSelectedMax){
                break;
            }
        }

        gameIntent.putExtra(Constants.selectedImgs, selectedImgs);
        unregisterReceiver(receiver);
        startActivity(gameIntent);
    }
}