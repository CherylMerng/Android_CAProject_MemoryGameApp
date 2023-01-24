package iss.workshop.ca;

import static android.content.Intent.FLAG_RECEIVER_FOREGROUND;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.webkit.URLUtil;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

public class DownloadService extends Service {
    private Thread backgroundThread;
    private Intent broadcastIntent = new Intent();
    private int serviceId;

    public DownloadService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        String action = intent.getAction();
        String url = intent.getStringExtra("url");
        serviceId = intent.getIntExtra(Constants.serviceId, 0);
        broadcastIntent.putExtra(Constants.serviceId, serviceId);

        if(!validateURL(url)){
            broadcastIntent.setAction(Constants.invalidUrl);
            sendBroadcast(broadcastIntent);
            action = null;
        }

        if(action != null){
            if(action.equals(Constants.action_download)){
                if(backgroundThread != null){
                    backgroundThread.interrupt();
                }

                backgroundThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String[] imageurls = new Scraper().getImageUrls(url);

                        if(imageurls != null) {
                            if (imageurls.length >= Constants.imgMax) {
                                for (int i = 0; i < imageurls.length; i++) {
                                    if (backgroundThread.interrupted()) {
                                        return;
                                    }

                                    if (MainActivity.imgCount >= Constants.imgMax) {
                                        break;
                                    }

                                    File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                                    String imgname = UUID.randomUUID().toString() + imageurls[i].substring(imageurls[i].lastIndexOf("."));
                                    File destFile = new File(dir, imgname);

                                    try {
                                        if (MainActivity.imgCount <= Constants.imgMax) {
                                            downloadImg(new URL(imageurls[i]), destFile);
                                            Thread.sleep(700);
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                                broadcastIntent.setAction(Constants.dwnComplete);
                                sendBroadcast(broadcastIntent);
                            } else if (imageurls.length < Constants.imgMax) {
                                broadcastIntent.setAction(Constants.dwnComplete);
                                sendBroadcast(broadcastIntent);
                                backgroundThread.interrupt();
                            }
                        }
                        else{
                            broadcastIntent.setAction(Constants.invalidUrl);
                            sendBroadcast(broadcastIntent);
                        }
                    }
                });

                backgroundThread.start();
            }
        }

        return START_NOT_STICKY;
    }

    public void downloadImg(URL url, File destFile) throws Exception{
        if(MainActivity.imgCount < Constants.imgMax){
            URLConnection conn = url.openConnection();

            InputStream in = conn.getInputStream();
            FileOutputStream out = new FileOutputStream(destFile);

            byte[] buf = new byte[4096];
            int bytesRead = -1;

            while ((bytesRead = in.read(buf)) != -1) {
                out.write(buf, 0, bytesRead);
            }

            out.close();
            in.close();

            broadcastIntent.setAction(Constants.setImage);
            broadcastIntent.setFlags(FLAG_RECEIVER_FOREGROUND);
            broadcastIntent.putExtra(Constants.imgPath, destFile.getAbsolutePath());
            sendBroadcast(broadcastIntent);
        }
    }

    @Override
    public void onDestroy(){
        if(backgroundThread != null){
            backgroundThread.interrupt();
        }
        super.onDestroy();
    }

    public boolean validateURL(String url){
        if(!URLUtil.isValidUrl(url)){
            return false;
        }

        return true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}