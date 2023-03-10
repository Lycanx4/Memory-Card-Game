package iss.workshop.android_ca;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import java.net.URI;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.stream.Collectors;

import kotlin.LateinitKt;

public class MainActivity2 extends AppCompatActivity implements AdapterView.OnItemClickListener, View.OnClickListener {



    // playing Grid
    GridView gridView;

    HashMap<String, Integer> leaderBoard2;

    // first image that is clicked
    ImageView firstImageSelected = null;
    int firstImage_Pos = -1; // 0-11

    // second image clicked
    ImageView secondImageSelected;
    int secondImage_Pos;

    // List of imageViews that have been matched
    List<ImageView> matched_imageViews = new ArrayList<>();

    //Count up timer
    Chronometer chronometer;
    private long pauseOffset;
    private boolean running;
    //turn
    int turn = 1;

    //score
    int p1_score = 0;
    int p2_score = 0;

    //Position List
    private ArrayList<String> text = new ArrayList<>();

    //Image Uri of 12 images (from select 6 images)
    private ArrayList<Uri> imageList = new ArrayList<>();

    TextView tv_p1;
    TextView tv_p2;
    TextView timer;

    // For displaying player mode pop-up
    Dialog playerModePopup;

    private SoundPool soundPool;
    int sound, correctsound;

    // Game object
    Game game;

    AnimatorSet setRightOut;
    AnimatorSet setLeftIn;
    AnimatorSet setRightOut2;
    AnimatorSet setLeftIn2;
    AnimatorSet setRightOut3;
    AnimatorSet setLeftIn3;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        tv_p1 = (TextView) findViewById(R.id.tv_ply1);
        tv_p2 = (TextView) findViewById(R.id.tv_ply2);

        leaderBoard2 = loadLeaderBoard();

        //starts from player 1
        tv_p1.setTextColor(Color.parseColor("#6305dc"));
        tv_p2.setTextColor(Color.GRAY);

        // this fills a list of 12 images, with the 2 selected images from internal storage
        fillArray();
        setRightOut =(AnimatorSet) AnimatorInflater.loadAnimator(getApplicationContext(),
                R.animator.front_animation);
        setLeftIn =(AnimatorSet) AnimatorInflater.loadAnimator(getApplicationContext(),
                R.animator.back_animation);
        setRightOut2 =(AnimatorSet) AnimatorInflater.loadAnimator(getApplicationContext(),
                R.animator.back_animation2);
        setLeftIn2 =(AnimatorSet) AnimatorInflater.loadAnimator(getApplicationContext(),
                R.animator.back_animation2);
        setRightOut3 =(AnimatorSet) AnimatorInflater.loadAnimator(getApplicationContext(),
                R.animator.front_animation3);
        setLeftIn3 =(AnimatorSet) AnimatorInflater.loadAnimator(getApplicationContext(),
                R.animator.back_animation3);


        // build sound pool
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(audioAttributes)
                .build();

        sound = soundPool.load(this,R.raw.flip,1);
        correctsound = soundPool.load(this,R.raw.correct,1);

        // Sets the playing grid
        gridView = (GridView) findViewById(R.id.gridView);
        ImageAdaptor imageAdaptor = new ImageAdaptor(this);
        gridView.setAdapter(imageAdaptor);
        gridView.setEnabled(false);


        // set onItemClickListener for playing Grid
        gridView.setOnItemClickListener(this);

        // set onClickListener for other buttons
        Button startBtn = findViewById(R.id.button_StartGame);
        startBtn.setOnClickListener(this);

        // instantiate dialog for popup
        playerModePopup = new Dialog(this);
        showPlayModePopup();
        Button gameModeBtn = findViewById(R.id.btn_ChangeMode);
        gameModeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPlayModePopup();
            }
        });

        // instantiate Game object
        game = new Game();
    }



    // FOR START GAME BUTTON
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_StartGame){

            gridView.setEnabled(true);
            Button startBtn = findViewById(R.id.button_StartGame);
            startBtn.setVisibility(view.GONE);

            Button gameModeBtn = findViewById(R.id.btn_ChangeMode);
            gameModeBtn.setVisibility(view.GONE);
            view.setEnabled(false); // view = start button

            timer = findViewById(R.id.playtime);
            chronometer = findViewById(R.id.playtime);
            chronometer.setFormat("Time: %s");

            startTimer();

//            new CountDownTimer(240 * 1000, 1000) {
////                public void onTick(long millisUntilFinished) {
////                    timer.setText("TIME : " + millisUntilFinished / 1000 + "s");
////                }
////
////                public void onFinish() {
////                    showGameResults();
////                }
////            }.start();

            if (game.getGameMode() == 1){
                TextView gameMode_tv = findViewById(R.id.mode_of_game);
                gameMode_tv.setText("SINGLE MODE");
                tv_p1.setVisibility(View.VISIBLE);
                tv_p1.setGravity(View.TEXT_ALIGNMENT_CENTER);
                tv_p2.setVisibility(View.GONE);
            }
            else if (game.getGameMode() == 0){
                tv_p1.setText(game.getPlayer1_name() + " : 0");
                tv_p2.setText(game.getPlayer2_name() + " : 0");
            }
        }
    }

    //Start Timer
    private void startTimer() {
        if(!running){
            chronometer.setBase(SystemClock.elapsedRealtime()-pauseOffset);
            chronometer.start();
            running = true;
        }
    }
    private void pauseTimer(){
        if(running){
            chronometer.stop();
            pauseOffset = SystemClock.elapsedRealtime()-chronometer.getBase();
            running = false;

            // set the timing for the single player
            long timing = pauseOffset / (1000); // convert milli to seconds
            DecimalFormat df = new DecimalFormat("#.###"); // format to 3 dp
            df.format(timing);
            if (game.getGameMode() == 1){
                game.setPlayer1_time(Math.toIntExact(timing));
            }
        }
    }
    private void resetTimer(){
        chronometer.setBase(SystemClock.elapsedRealtime());
        pauseOffset = 0;
    }

    // FOR GRID VIEW
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        ImageView img_view = (ImageView) view;

        //first clicked
        if (firstImage_Pos < 0) {

            // Display image

            firstImage_Pos = position;
            firstImageSelected = img_view;
            setRightOut.setTarget(firstImageSelected);
            setLeftIn.setTarget(R.drawable.question_mark);
            setRightOut.start();
            setLeftIn.start();
            PlaySound(sound);
            img_view.setImageURI(imageList.get(position));
            return;
        }

        // second click
        // 1) Display 2nd image clicked. Always true
        if (firstImage_Pos != position) {
            PlaySound(sound);
            secondImage_Pos = position;
            secondImageSelected = img_view;


            img_view.setImageURI(imageList.get(position));
            setRightOut2.setTarget(secondImageSelected);
            setLeftIn2.setTarget(R.drawable.question_mark);
            setRightOut2.start();
            setLeftIn2.start();


        }
        gridView.setEnabled(false);
        // 2) check if the 2 images are the same.
        // if not same, change back.
        // if same, give score.
        // finally, change turn and reset firstImage_Pos = -1.
        Runnable checker = new Runnable() {
            @Override
            public void run() {

                checkImagesAndScore();
                gridView.setEnabled(true);
            }
        };

        img_view.postDelayed(checker, 2000);

    }

    private void PlaySound(int soundId) {
        if(soundId == sound){

            soundPool.play(sound,1,1,0,0,1);
        }else{
                soundPool.play(correctsound,1,1,0,0,1);
        }
    }

    private void checkImagesAndScore(){
        String firstImageUri = imageList.get(firstImage_Pos).getPath();
        String secondImageUri = imageList.get(secondImage_Pos).getPath();

        if (!firstImageUri.equals(secondImageUri)) {
            // if not same
            if (!matched_imageViews.contains(firstImageSelected)){
                firstImageSelected.setImageResource(R.drawable.question_mark);
            }
            if (!matched_imageViews.contains(secondImageSelected)){
                secondImageSelected.setImageResource(R.drawable.question_mark);
            }

            setRightOut.setTarget(R.drawable.question_mark);
            setLeftIn.setTarget(firstImageSelected);
            setRightOut.start();
            setLeftIn.start();


            setRightOut3.setTarget(R.drawable.question_mark);
            setLeftIn3.setTarget(secondImageSelected);
            setRightOut3.start();
            setLeftIn3.start();

            firstImageSelected.setImageResource(R.drawable.question_mark);
            secondImageSelected.setImageResource(R.drawable.question_mark);
        }
        else{
            // if same
            PlaySound(correctsound);
            firstImageSelected.setOnClickListener(null);
            secondImageSelected.setOnClickListener(null);

            matched_imageViews.add(firstImageSelected);
            matched_imageViews.add(secondImageSelected);

            // for multi mode: give score
            if (turn == 1) {
                p1_score++;

                if (game.getGameMode() == 0){
                    tv_p1.setText(game.getPlayer1_name() + " : " + p1_score);
                }
            } else if (turn == 2) {
                p2_score++;

                if (game.getGameMode() == 0){
                    tv_p2.setText(game.getPlayer2_name() + " : " + p2_score);
                }
            }

            if(p1_score+p2_score==6){
                game.setPlayer1_score(p1_score);
                game.setPlayer2_score(p2_score);

                pauseTimer();
                resetTimer();
                showGameResults();
            }
        }

        // finally, reset firstImage clicked position and change turn
        firstImage_Pos = -1;

        if (game.getGameMode() == 0){
//            if (turn == 1) {
//                turn = 2;
//                tv_p1.setTextColor(Color.GRAY);
//                tv_p2.setTextColor(Color.GREEN);
//            } else if (turn == 2) {
//                turn = 1;
//                tv_p2.setTextColor(Color.GRAY);
//                tv_p1.setTextColor(Color.GREEN);
//            }
            if (turn == 1) {
                turn = 2;
                tv_p1.setTextColor(Color.GRAY);
                tv_p2.setTextColor(Color.parseColor("#6305dc"));
            } else if (turn == 2) {
                turn = 1;
                tv_p2.setTextColor(Color.GRAY);
                tv_p1.setTextColor(Color.parseColor("#6305dc"));
            }
        }
    }

    public void fillArray() {

        // put integers 0-11 into array, representing the positions of the images in the grid
        // global attribute: text: List<Integer>
        for (Integer i = 0; i < 12; i++) {
            text.add(i.toString());
        }

        // put 12 resource Id of images into a list
        // global attribute: imageList: List<Integer>
        String filePath = "selected_6";
        File mTargetFolder = new File(getFilesDir(), filePath);

        if (mTargetFolder.exists()){
            File[] selected_6_images = mTargetFolder.listFiles();

            if (selected_6_images != null){
                for (int i=0; i<2; i++){
                    for (int j=0; j<6; j++){
                        File imageFile = selected_6_images[j];
                        Uri imageUri = Uri.fromFile(imageFile);
                        imageList.add(imageUri);
                    }
                }
            }
        }

        Collections.shuffle(imageList);

    }

    private void showGameResults(){

        String singleMode_msg = "Game Over!\n"
                                + game.getPlayer1_name() + "'s timing: \n"
                                + game.getPlayer1_time() + " sec";

        String multiMode_msg = "Game Over!\n"
                + game.getPlayer1_name() + " : " + p1_score
                + "\n" + game.getPlayer2_name() + " : " + p2_score;

        String gameEnd_msg = "";

        if (game.getGameMode() == 1){
            gameEnd_msg = singleMode_msg;
        }
        else if (game.getGameMode() == 0){
            gameEnd_msg = multiMode_msg;
        }

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity2.this);
        alertDialogBuilder
                .setMessage(gameEnd_msg)
                .setCancelable(false)
                .setPositiveButton("EXIT", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton("RESTART", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(getApplicationContext(),MainActivity2.class);
                        startActivity(intent);
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

        //For Leader Board
        game.setPlayer1_score(p1_score);
        game.setPlayer2_score(p2_score);

        IsAchieveLeaderBoard();

        String abc = "abc";
    }

    private void showPlayModePopup(){
        TextView textClose;
        Button singleMode;
        Button multiMode;
        EditText player1_namefield;
        EditText player2_namefield;
        Button confirmBtn;

        playerModePopup.setContentView(R.layout.game_mode_popup);
        textClose = (TextView) playerModePopup.findViewById(R.id.popup_close_textView);
        singleMode = (Button) playerModePopup.findViewById(R.id.single_player_btn);
        multiMode = (Button) playerModePopup.findViewById(R.id.multi_player_btn);
        player1_namefield = (EditText) playerModePopup.findViewById(R.id.player_1_name_field);
        player2_namefield = (EditText) playerModePopup.findViewById(R.id.player_2_name_field);
        confirmBtn = (Button) playerModePopup.findViewById(R.id.name_confirm_btn);

        textClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playerModePopup.dismiss();
            }
        });

        singleMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                player1_namefield.setEnabled(true);
//                player2_namefield.setEnabled(false);
                player2_namefield.setVisibility(View.GONE);
                confirmBtn.setVisibility(view.VISIBLE);

                // set game mode for game object, instantiated oncreate
                game.setGameMode(1);
            }
        });

        multiMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                player1_namefield.setEnabled(true);
                player2_namefield.setVisibility(View.VISIBLE);
                player2_namefield.setEnabled(true);
                confirmBtn.setVisibility(view.VISIBLE);

                // set game mode for game object, instantiated oncreate
                game.setGameMode(0);
            }
        });

        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // set player names
                String name1 = player1_namefield.getText().toString();
                String name2 = player2_namefield.getText().toString();
                String player1Name = (name1.isEmpty() || name1 == null) ? "Player 1" : name1;
                String player2Name = (name2.isEmpty() || name2 == null) ? "Player 2" : name2;

                // set default visibility for player names (for clicking confirm button multiple times)
                tv_p1.setVisibility(View.VISIBLE);
                tv_p2.setVisibility(View.VISIBLE);

                if (game.getGameMode() == 0){
                    // set player names
                    game.setPlayer1_name(player1Name);
                    game.setPlayer2_name(player2Name);

                    // set layout for multiplayer
                    TextView gameMode_tv = findViewById(R.id.mode_of_game);
                    gameMode_tv.setText("VS MODE");
                    tv_p1.setText(game.getPlayer1_name() + " : 0");
                    tv_p2.setText(game.getPlayer2_name() + " : 0");
                }
                else if (game.getGameMode() == 1){
                    // set player 1 name
                    game.setPlayer1_name(player1Name);

                    // set layout for single player
                    TextView gameMode_tv = findViewById(R.id.mode_of_game);
                    gameMode_tv.setText("SINGLE MODE");
                    tv_p1.setText("Player: " + game.getPlayer1_name());
//                    tv_p1.setVisibility(View.INVISIBLE);
                    tv_p2.setVisibility(View.INVISIBLE);
                }

                // activate start button in main game
                Button startBtn_mainGame = findViewById(R.id.button_StartGame);
                startBtn_mainGame.setEnabled(true);

                playerModePopup.dismiss();
            }
        });

        playerModePopup.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        playerModePopup.show();
    }
    protected boolean IsAchieveLeaderBoard(){
        Boolean p1Win = true;

        if(game.getPlayer2_name() != null){
            return false;
        }
//        if(game.getPlayer2_name() != null){
//            // multi
//            p1Win = game.getPlayer1_score()> game.getPlayer2_score();
//
//        }else if(game.getPlayer2_name() == null && game.getPlayer2_score() != 0){
//            // single
//            int temp = game.getPlayer1_score() + game.getPlayer2_score();
//            game.setPlayer1_score(temp);
//            game.setPlayer2_score(0);
//        }

        if(leaderBoard2 == null){
            leaderBoard2 = new HashMap<>();
        }

        if(leaderBoard2.size() < 10){
            if(p1Win){
                leaderBoard2.put(game.getPlayer1_name(),Math.toIntExact(game.getPlayer1_time()));
            }else{
                leaderBoard2.put(game.getPlayer2_name(),Math.toIntExact(game.getPlayer2_time()));
            }
            saveLeaderBoard(leaderBoard2);
            leaderBoard2 = loadLeaderBoard();
            return true;
        }

        if(leaderBoard2.size()==10){

            String cPName = new ArrayList<>(leaderBoard2.keySet()).get(9); // get the one at the lowest position
            Integer cPTime = leaderBoard2.get(cPName);

            if(game.getPlayer1_score()>cPTime || game.getPlayer2_score()>cPTime){
                leaderBoard2.remove(cPName);
                saveLeaderBoard(leaderBoard2);
                IsAchieveLeaderBoard();
            }
        }

        return false;
    }

    protected void saveLeaderBoard(@NonNull HashMap<String,Integer> scoreList){
        SharedPreferences splb = getSharedPreferences("leaderBoard", MODE_PRIVATE);

        SharedPreferences.Editor editor = splb.edit();

//        HashMap<String, Integer> sortedHM = scoreList.entrySet().stream()
//                .sorted((x, y)-> y.getValue().compareTo(x.getValue())).collect(Collectors
//                        .toMap(Map.Entry::getKey,Map.Entry::getValue,(e1, e2) -> e1, LinkedHashMap::new));

        HashMap<String, Integer> sortedHM = scoreList.entrySet().stream()
                .sorted(Map.Entry.comparingByValue()).collect(Collectors
                        .toMap(Map.Entry::getKey,Map.Entry::getValue,(e1, e2) -> e1, LinkedHashMap::new));

        List<String> lbNames = new ArrayList<>(sortedHM.keySet());
        List<Integer> lbScores = new ArrayList<>(sortedHM.values());

        for(int i=0; i<sortedHM.size(); i++){
            editor.putInt(lbNames.get(i),lbScores.get(i));
            editor.putStringSet("lbNames", sortedHM.keySet());
            editor.commit();
        }

    }

    protected HashMap<String,Integer> loadLeaderBoard(){
        SharedPreferences splb = getSharedPreferences("leaderBoard", MODE_PRIVATE);
        List<String> lbNames = new ArrayList<>(splb.getStringSet("lbNames", new HashSet<>()));

        HashMap<String,Integer> scoreList = new HashMap<>();
        if(lbNames.size()>0) {
            for (int i = 0; i < lbNames.size(); i++) {
                String nam = lbNames.get(i);
                scoreList.put(nam, splb.getInt(nam,0));
            }
        }
//        HashMap<String, Integer> sortedHM = scoreList.entrySet().stream()
//                .sorted((x, y)-> y.getValue().compareTo(x.getValue())).collect(Collectors
//                        .toMap(Map.Entry::getKey,Map.Entry::getValue,(e1, e2) -> e1, LinkedHashMap::new));
        HashMap<String, Integer> sortedHM = scoreList.entrySet().stream()
                .sorted(Map.Entry.comparingByValue()).collect(Collectors
                        .toMap(Map.Entry::getKey,Map.Entry::getValue,(e1, e2) -> e1, LinkedHashMap::new));


        return sortedHM;
    }
}


