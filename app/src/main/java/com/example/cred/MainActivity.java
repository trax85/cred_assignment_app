package com.example.cred;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttp;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import pl.droidsonroids.gif.GifImageView;

public class MainActivity extends AppCompatActivity {
    private ImageView dragView;
    private TextView successText;
    private ImageView downArrow, dropContainer;
    private Button button;
    private Animation fadeInAnimation;
    private Animation floatingAnimation;
    private Animation translateDown;
    private Animation pulseAnimation;
    private static float originalY;
    private float yAxis;
    private static float containerYStart;
    private GifImageView gifImageView;
    private CardView cardView;
    private LinearLayout expandLayout;
    private OkHttpClient client;
    private String url;
    private final String endPoint1 = "https://api.mocklets.com/p68348/success_case";
    private final String endPoint2 = "https://api.mocklets.com/p68348/failure_case";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        client = new OkHttpClient();
        url = endPoint1;
        initViews();
        initEventListeners();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        downArrow.setColorFilter(Color.parseColor("#008080"));
        dropContainer.setColorFilter(Color.parseColor("#2E2E2E"));
        originalY = dragView.getY();
        containerYStart = dropContainer.getY();
    }

    private void initViews(){
        dragView = findViewById(R.id.drag_down_ic);
        successText = findViewById(R.id.success_text);
        downArrow = findViewById(R.id.down_arrow);
        dropContainer = findViewById(R.id.drop_container);
        gifImageView = findViewById(R.id.loader_gif);
        expandLayout = findViewById(R.id.expand_linear_layout);
        cardView = findViewById(R.id.expandable_cardview);
        button = findViewById(R.id.toggle_button);
    }

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    private void initEventListeners(){
        dragView.setOnTouchListener(new DragTouchListener());
        floatingAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.float_anim);
        fadeInAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.reveal_anim);
        pulseAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in_out);
        translateDown = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.translate_down);
        dragView.setAnimation(floatingAnimation);
        downArrow.setAnimation(pulseAnimation);
        button.setOnClickListener(v -> {
            if(url.contains(endPoint1)) {
                button.setText("Failure");
                url = endPoint2;
            }
            else {
                button.setText("Success");
                url = endPoint1;
            }
        });
    }

    private class DragTouchListener implements View.OnTouchListener{

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            if(event.getAction() == MotionEvent.ACTION_DOWN){
                dragView.setAnimation(null);
                yAxis = event.getRawY();
            }

            if(event.getAction() == MotionEvent.ACTION_UP){
                if(isIcDroppedInContainer(dragView.getY()))
                    makeNetworkCall(url);
                else dragView.setY(originalY);
            }

            if(event.getAction() == MotionEvent.ACTION_MOVE ){
                float animY = event.getRawY() - yAxis;
                //Don't allow cred ic to move above its original Y axis co-ordinates
                if(dragView.getY() + animY < originalY)
                    return false;
                dragView.setY(dragView.getY() + animY);
                yAxis = event.getRawY();

                return true;
            }
            return false;
        }
    }

    private boolean isIcDroppedInContainer(float y){
        //keep small buffer to make sure user has more chances of dropping it into container
        float buffer  = 10;
        return y > (containerYStart - buffer) && y < (containerYStart + buffer);
    }

    private void successCase(){
        downArrow.setAnimation(null);
        translateDown.setFillAfter(true);
        gifImageView.setVisibility(View.GONE);
        dropContainer.setAnimation(translateDown);

        TransitionManager.beginDelayedTransition(cardView, new AutoTransition());
        expandLayout.setVisibility(View.VISIBLE);
        fadeInAnimation.setFillAfter(true);
        successText.startAnimation(fadeInAnimation);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void failureCase(){
        gifImageView.setVisibility(View.GONE);
        Toast.makeText(getApplicationContext(), "Failed",Toast.LENGTH_LONG).show();

        downArrow.setVisibility(View.VISIBLE);
        downArrow.setAnimation(pulseAnimation);

        dragView.setY(originalY);
        dragView.setVisibility(View.VISIBLE);
        dragView.setAnimation(floatingAnimation);
    }

    private void makeNetworkCall(String url)
    {
        downArrow.setAnimation(null);
        downArrow.setVisibility(View.INVISIBLE);
        gifImageView.setVisibility(View.VISIBLE);
        dragView.setVisibility(View.INVISIBLE);

        Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d("DEBUG", "Fetch request failed");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()){
                    Log.d("DEBUG", "Fetch request success");
                    ResponseBody responseBody = response.body();
                    Handler handler = new Handler(Looper.getMainLooper());

                    try {
                        JSONObject object = new JSONObject(responseBody.string());
                        String state = String.valueOf(object.get("success"));
                        if(state.contains("true"))
                            handler.post(() -> successCase());
                        else
                            handler.post(() -> failureCase());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}