package iss.workshop.ca;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import nl.dionsegijn.konfetti.core.Angle;
import nl.dionsegijn.konfetti.core.Party;
import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.core.emitter.EmitterConfig;
import nl.dionsegijn.konfetti.xml.KonfettiView;

public class GameActivity extends AppCompatActivity {
    private RecyclerAdaptor recyclerAdaptor;
    private String[] images, allimages, placeholders = new String[Constants.imgSelectedMax*2];
    private List<String> fullimages;
    public static boolean clickable, mute = false;
    private boolean uiclickable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        clickable = false;
        uiclickable = true;

        Intent gameIntent = getIntent();
        images = gameIntent.getStringArrayExtra(Constants.selectedImgs);

        if(images == null){
            Toast.makeText(this, "An error has occurred.", Toast.LENGTH_LONG);
            returnMain();
        }

        if(images.length < Constants.imgSelectedMax){
            Toast.makeText(this, "Insufficient images.", Toast.LENGTH_LONG);
            returnMain();
        }

        prepareImgs();
        setRecycler();
        setSoundBtn();
        setBtns();
        registerReceivers();
    }

    @Override
    public void onStart(){
        super.onStart();

        setPrepScreen();
    }

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action == null){
                return;
            }

            if(action.equals(Constants.gameDone)){
                clickable = false;
                uiclickable = false;

                Chronometer timer = findViewById(R.id.timer);
                timer.stop();
                long time = SystemClock.elapsedRealtime() - timer.getBase();
                DecimalFormat df = new DecimalFormat("00");
                String currTime = df.format(TimeUnit.MILLISECONDS.toMinutes(time)) + " mins " + df.format(TimeUnit.MILLISECONDS.toSeconds(time)%60) + " secs";

                LinearLayout overlay = findViewById(R.id.overlay);
                overlay.setVisibility(View.VISIBLE);

                if(GameActivity.mute == false){
                    int resourceId = getResources().getIdentifier("cheer", "raw", getPackageName());
                    MediaPlayer mp = MediaPlayer.create(GameActivity.this, resourceId);
                    mp.start();
                }

                int resourceId = getResources().getIdentifier("awards", "raw", getPackageName());
                ImageView awarddisp = findViewById(R.id.awardimg);
                awarddisp.setImageResource(resourceId);
                ObjectAnimator animator = ObjectAnimator.ofFloat(awarddisp, "translationY", 100f);
                animator.setInterpolator(new BounceInterpolator());
                animator.setDuration(1000);
                animator.start();
                awarddisp.setVisibility(View.VISIBLE);

                awarddisp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        animator.setDuration(500);
                        animator.start();
                    }
                });

                TextView bigtxt = findViewById(R.id.fillertext);
                TextView oldscoretxt = findViewById(R.id.prevscoretext);
                bigtxt.setTextSize(TypedValue.COMPLEX_UNIT_SP,30);
                bigtxt.setText(Constants.winnerMsg);

                long oldscore = getScore();
                if(oldscore == 0){
                    setScore(time);
                }
                else{
                    if(oldscore > time){
                        oldscoretxt.setText("Best Time: " + currTime);
                        currTime = currTime + "\n(NEW BEST TIME!)";
                        setScore(time);
                    }
                    else {
                        String oldscorestring = df.format(TimeUnit.MILLISECONDS.toMinutes(oldscore)) + " mins " + df.format(TimeUnit.MILLISECONDS.toSeconds(oldscore)%60) + " secs";
                        oldscoretxt.setText("Best Time: " + oldscorestring);
                    }
                }

                TextView scoretxt = findViewById(R.id.scoretext);
                scoretxt.setText("Time: " + currTime);
                scoretxt.setVisibility(View.VISIBLE);
                oldscoretxt.setVisibility(View.VISIBLE);

                Button btn1 = findViewById(R.id.homebtn);
                Button btn2 = findViewById(R.id.replaybtn);

                KonfettiView konfettiView = findViewById(R.id.viewConfetti);
                EmitterConfig emitterConfig = new Emitter(5, TimeUnit.SECONDS).perSecond(30);
                Party party = new PartyFactory(emitterConfig)
                        .angle(Angle.TOP)
                        .position(0.5, 0)
                        .build();
                konfettiView.start(party);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        btn1.setVisibility(View.VISIBLE);
                        btn2.setVisibility(View.VISIBLE);
                    }
                }, 1500);
            }
        }
    };

    protected void registerReceivers(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.gameDone);
        registerReceiver(receiver, filter);
    }

    protected void prepareImgs(){
        fullimages = new ArrayList<>(Arrays.asList(images));
        fullimages.addAll(new ArrayList<>(Arrays.asList(images)));
        Collections.shuffle(fullimages);
        allimages = fullimages.toArray(new String[fullimages.size()]);
    }

    protected void setRecycler(){
        RecyclerView recyclerView = findViewById(R.id.recyclerGame);
        recyclerView.setLayoutManager(new GridLayoutManager(GameActivity.this, Constants.gameCol));

        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File dImg = new File(dir, "defaultimg.png");
        String path = dImg.getAbsolutePath();
        Arrays.fill(placeholders, path);

        recyclerAdaptor = new RecyclerAdaptor(placeholders);
        recyclerAdaptor.setTrueimages(allimages);
        recyclerView.setAdapter(recyclerAdaptor);
    }

    protected void setSoundBtn(){
        ImageView soundBtn = findViewById(R.id.soundbtn);
        soundBtn.setImageResource(getResources().getIdentifier("unmute", "raw", getPackageName()));
        soundBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(uiclickable == true){
                    if(mute == false){
                        soundBtn.setImageResource(getResources().getIdentifier("mute", "raw", getPackageName()));
                        mute = true;
                    }
                    else{
                        soundBtn.setImageResource(getResources().getIdentifier("unmute", "raw", getPackageName()));
                        mute = false;
                    }
                }
            }
        });
    }

    protected void setBtns(){
        Button btn1 = findViewById(R.id.returnbtn);
        Button btn2 = findViewById(R.id.homebtn);

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(uiclickable == true){
                    DialogInterface.OnClickListener dialogueListener= new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            switch (i){
                                case DialogInterface.BUTTON_POSITIVE:
                                    returnMain();

                                case DialogInterface.BUTTON_NEGATIVE:
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
                    builder.setMessage("Return to Home?")
                            .setPositiveButton("Yes", dialogueListener)
                            .setNegativeButton("No", dialogueListener)
                            .show();
                }
            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                returnMain();
            }
        });

        Button btn3 = findViewById(R.id.replaybtn);
        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                replay();
            }
        });
    }

    protected void setPrepScreen(){
        TextView fillertext = findViewById(R.id.fillertext);
        LinearLayout overlay = findViewById(R.id.overlay);
        Chronometer timer = findViewById(R.id.timer);

        fillertext.setText("Ready?");

        CountDownTimer countDownTimer = new CountDownTimer(4000, 100){
            int seconds = 0;

            @Override
            public void onTick(long secsLeft){

                if(Math.round((float)secsLeft / 1000.0f) != seconds){
                    seconds = Math.round((float)secsLeft / 1000.0f);
                    if(secsLeft/1000 != 0){
                        fillertext.setText("" + secsLeft/1000);
                    }
                    else{
                        fillertext.setText("Go!");
                    }
                }
            }

            @Override
            public void onFinish(){
                overlay.setVisibility(View.GONE);
                clickable = true;
                timer.setBase(SystemClock.elapsedRealtime());
                timer.start();
            }
        };

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                countDownTimer.start();
            }
        }, 1000);
    }

    protected long getScore(){
        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
        Long score = pref.getLong("score", 0);
        return score;
    }

    protected void setScore(long score){
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong("score", score);
        editor.commit();
    }

    protected void returnMain(){
        Intent returnIntent = new Intent(GameActivity.this, MainActivity.class);
        unregisterReceiver(receiver);
        startActivity(returnIntent);
    }

    protected void replay(){
        Intent restartIntent = new Intent(GameActivity.this, GameActivity.class);
        restartIntent.putExtra(Constants.selectedImgs, images);
        unregisterReceiver(receiver);
        startActivity(restartIntent);
    }
}