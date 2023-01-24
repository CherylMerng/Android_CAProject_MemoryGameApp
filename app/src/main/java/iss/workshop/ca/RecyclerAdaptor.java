package iss.workshop.ca;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.CycleInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;

public class RecyclerAdaptor extends RecyclerView.Adapter<RecyclerAdaptor.ViewHolder> {
    private String[] images, trueimages;
    private String prevSelectedImg;
    private boolean[] clicked;
    private int clicks = 0, prevPosition, correct = 0;
    private ViewHolder prevHolder;
    private ObjectAnimator animator;
    private MediaPlayer mp;

    public static class ViewHolder extends RecyclerView.ViewHolder{
        private final ImageView imageView;
        private final TextView textView;

        public ViewHolder(View view){
            super(view);
            imageView = (ImageView) view.findViewById(R.id.imagedisp);
            textView = (TextView) view.findViewById(R.id.numbering);
        }

        public ImageView getImageView(){
            return imageView;
        }
        public TextView getTextView(){return textView;}
    }

    public RecyclerAdaptor(String[] images){
        this.images = images;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType){
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.grid_cell, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, @SuppressLint("RecyclerView") int position){
        Bitmap bitmap = BitmapFactory.decodeFile(images[position]);
        bitmap = Bitmap.createScaledBitmap(bitmap, Constants.imgWidthMax, Constants.imgHeightMax, false);
        viewHolder.getImageView().setImageBitmap(bitmap);

        if(viewHolder.getImageView().getContext().getClass() == MainActivity.class){
            viewHolder.getImageView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(MainActivity.imgCount == Constants.imgMax){
                        if(MainActivity.imgSelectedCount < Constants.imgSelectedMax){
                            Intent broadcastIntent = new Intent();

                            if(viewHolder.getImageView().getAlpha() != 0.5f){
                                viewHolder.getImageView().setAlpha(0.5f);
                                viewHolder.getImageView().setBackgroundResource(R.drawable.border_image);
                                MainActivity.imgSelectedCount++;
                                viewHolder.getTextView().setText("" + MainActivity.imgSelectedCount);
                                viewHolder.getTextView().setVisibility(View.VISIBLE);

                                broadcastIntent.setAction(Constants.setSelected);
                                broadcastIntent.putExtra(Constants.pos, position);
                            }
                            else{
                                viewHolder.getImageView().setAlpha(1.0f);
                                viewHolder.getImageView().setBackgroundResource(0);
                                viewHolder.getTextView().setVisibility(View.INVISIBLE);
                                int selectedBoxNum = Integer.parseInt(viewHolder.getTextView().getText().toString());
                                if(selectedBoxNum < MainActivity.imgSelectedCount){
                                    for(int i = selectedBoxNum + 1; i <= MainActivity.imgSelectedCount; i++){
                                        ArrayList<View> views = new ArrayList<>();
                                        view.getRootView().findViewsWithText(views, "" + i, View.FIND_VIEWS_WITH_TEXT);
                                        if(views.size() > 0){
                                            TextView target = (TextView) views.get(0);
                                            target.setText("" + (i - 1));
                                        }
                                    }
                                }
                                viewHolder.getTextView().setText("");
                                MainActivity.imgSelectedCount--;
                                broadcastIntent.setAction(Constants.setUnselected);
                                broadcastIntent.putExtra(Constants.pos, position);
                            }

                            view.getContext().sendBroadcast(broadcastIntent);
                        }
                    }
                }
            });
        }
        else if(viewHolder.getImageView().getContext().getClass() == GameActivity.class){
            viewHolder.getImageView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if(clicked[position] == false && GameActivity.clickable == true){
                                clicks++;
                                if(clicks > 2){
                                    return;
                                }

                                ScaleAnimation shrink = new ScaleAnimation(1f, 0, 1f, 1f, 0.5f, 0);
                                shrink.setDuration(500);
                                viewHolder.getImageView().startAnimation(shrink);

                                Bitmap bitmap = BitmapFactory.decodeFile(trueimages[position]);
                                bitmap = Bitmap.createScaledBitmap(bitmap, Constants.imgWidthMax, Constants.imgHeightMax, false);
                                viewHolder.getImageView().setImageBitmap(bitmap);

                                ScaleAnimation expand = new ScaleAnimation(0 , 1f, 1f, 1f, 0.5f, 0);
                                expand.setDuration(500);
                                viewHolder.getImageView().startAnimation(expand);

                                if(clicks == 1){
                                    prevSelectedImg = trueimages[position];
                                    prevPosition = position;
                                    clicked[position] = true;
                                    prevHolder = viewHolder;
                                }
                                else{
                                    GameActivity.clickable = false;

                                    if(prevSelectedImg.equals(trueimages[position])){
                                        clicked[position] = true;
                                        correct++;

                                        TextView countdisp = view.getRootView().getRootView().findViewById(R.id.matchNum);
                                        countdisp.setText(correct + " of " + Constants.imgSelectedMax + " matches");

                                        if(GameActivity.mute == false){
                                            int resourceId = view.getResources().getIdentifier("correct", "raw", view.getContext().getPackageName());
                                            mp = MediaPlayer.create(view.getContext(), resourceId);
                                            mp.start();
                                        }

                                        clicks = 0;
                                        GameActivity.clickable = true;

                                        if(correct == 6){
                                            Intent gameDone = new Intent();
                                            gameDone.setAction(Constants.gameDone);
                                            view.getContext().sendBroadcast(gameDone);
                                        }
                                    }
                                    else{
                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                clicked[prevPosition] = false;

                                                if(GameActivity.mute == false){
                                                    int resourceId = view.getResources().getIdentifier("wrongsound", "raw", view.getContext().getPackageName());
                                                    mp = MediaPlayer.create(view.getContext(), resourceId);
                                                    mp.start();
                                                }

                                                ScaleAnimation shrink = new ScaleAnimation(1f, 0, 1f, 1f, 0.5f, 0);
                                                shrink.setDuration(300);
                                                viewHolder.getImageView().startAnimation(shrink);
                                                prevHolder.getImageView().startAnimation(expand);

                                                Bitmap bits = BitmapFactory.decodeFile(images[0]);
                                                bits = Bitmap.createScaledBitmap(bits, Constants.imgWidthMax, Constants.imgHeightMax, false);
                                                viewHolder.getImageView().setImageBitmap(bits);
                                                prevHolder.getImageView().setImageBitmap(bits);

                                                ScaleAnimation expand = new ScaleAnimation(0 , 1f, 1f, 1f, 0.5f, 0);
                                                expand.setDuration(300);
                                                viewHolder.getImageView().startAnimation(expand);
                                                prevHolder.getImageView().startAnimation(expand);

                                                clicks = 0;
                                                GameActivity.clickable = true;
                                            }
                                        }, 1500);
                                    }
                                }
                            }
                            else{
                                animator = ObjectAnimator.ofFloat(viewHolder.getImageView(), "translationY", 5f);
                                animator.setInterpolator(new CycleInterpolator(5));
                                animator.setDuration(200);
                                animator.start();

                                if(GameActivity.mute == false){
                                    int resourceId = view.getResources().getIdentifier("click", "raw", view.getContext().getPackageName());
                                    mp = MediaPlayer.create(view.getContext(), resourceId);
                                    mp.start();
                                }
                            }
                        }
                    }, 500);
                }
            });
        }
    }

    public void setImage(int position, String url){
        if(position < images.length){
            images[position] = url;
            notifyItemChanged(position);
        }
    }

    public void setTrueimages(String[] trueimages){
        this.trueimages = trueimages;
        clicked = new boolean[trueimages.length];
        Arrays.fill(clicked, false);
    }

    @Override
    public int getItemCount(){
        return images.length;
    }
}