package com.example.tabliczkamnozenia;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {

    private static final int MIN_TABLE = 1;
    private static final int MAX_TABLE = 10;
    private static final int MIN_MULTIPLIER = 1;
    private static final int MAX_MULTIPLIER = 10;
    private static final int REQUIRED_STREAK = 5;

    private static final int MODE_MENU = 0;
    private static final int MODE_LEARNING = 1;
    private static final int MODE_TEST = 2;
    private static final int MODE_DAILY = 3;

    private static final int TEST_QUESTION_COUNT = 20;
    private static final int DAILY_QUESTION_COUNT = 10;
    private static final int TEST_MIN_NUMBER = 1;
    private static final int TEST_MAX_NUMBER = 10;

    private final Random random = new Random();
    private final ArrayList<Integer> currentSeries = new ArrayList<>();
    private final ArrayList<Question> testQuestions = new ArrayList<>();

    private final String[] happyMessages = new String[] {
            "Super!", "Brawo!", "Ekstra!", "Świetnie!", "Ale moc!", "Tak trzymaj!"
    };

    private final String[] tryAgainMessages = new String[] {
            "Nic nie szkodzi, próbujemy dalej!", "Dasz radę!", "Jeszcze raz i będzie super!"
    };

    private SharedPreferences prefs;

    private TextView titleTextView;
    private TextView subtitleTextView;
    private TextView starsTextView;
    private TextView badgesTextView;
    private TextView robotTextView;
    private ScrollView mainScrollView;
    private LinearLayout menuContainer;
    private LinearLayout quizContainer;
    private LinearLayout bottomAnswerBar;

    private TextView quizTitleTextView;
    private TextView progressTextView;
    private TextView questionTextView;
    private EditText answerEditText;
    private Button checkButton;
    private Button nextButton;
    private Button rulesButton;
    private Button retakeButton;
    private ImageButton backButton;
    private TextView resultTextView;
    private ImageView robotFaceImageView;
    private LinearLayout rootLayout;
    private FrameLayout adViewContainer;
    private AdView adView;
    private InterstitialAd interstitialAd;

    private final Button[] tableButtons = new Button[MAX_TABLE + 1];
    private final TextView[] statusTextViews = new TextView[MAX_TABLE + 1];

    private TextView testStatusTextView;
    private TextView dailyStatusTextView;

    private int currentMode = MODE_MENU;
    private int selectedTable = 0;
    private int currentMultiplier = 0;
    private int currentSeriesIndex = 0;

    private int currentLeftNumber = 0;
    private int currentRightNumber = 0;

    private int currentTestIndex = 0;
    private int currentTestCorrectAnswers = 0;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private long testStartTimeMillis = 0L;
    private int testWrongAnswers = 0;
    private boolean testTimerRunning = false;

    private int lastBadgeCount = -1;

    private boolean keyboardListenerReady = false;
    private boolean currentLearningRetake = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("tabliczka_mnozenia_dane", MODE_PRIVATE);

        bindViews();
        applySystemBarsInset();
        styleStaticButtons();
        setupAds();
        createMenuButtons();
        setupQuizButtons();
        showMenu();
    }

    private void bindViews() {
        rootLayout = findViewById(R.id.rootLayout);
        titleTextView = findViewById(R.id.titleTextView);
        subtitleTextView = findViewById(R.id.subtitleTextView);
        starsTextView = findViewById(R.id.starsTextView);
        badgesTextView = findViewById(R.id.badgesTextView);
        robotTextView = findViewById(R.id.robotTextView);
        mainScrollView = findViewById(R.id.mainScrollView);
        menuContainer = findViewById(R.id.menuContainer);
        quizContainer = findViewById(R.id.quizContainer);
        bottomAnswerBar = findViewById(R.id.bottomAnswerBar);

        quizTitleTextView = findViewById(R.id.quizTitleTextView);
        progressTextView = findViewById(R.id.progressTextView);
        questionTextView = findViewById(R.id.questionTextView);
        answerEditText = findViewById(R.id.answerEditText);
        checkButton = findViewById(R.id.checkButton);
        nextButton = findViewById(R.id.nextButton);
        rulesButton = findViewById(R.id.rulesButton);
        retakeButton = findViewById(R.id.retakeButton);
        backButton = findViewById(R.id.backButton);
        resultTextView = findViewById(R.id.resultTextView);
        robotFaceImageView = findViewById(R.id.robotFaceImageView);
        adViewContainer = findViewById(R.id.adViewContainer);
    }

    private void applySystemBarsInset() {
        if (rootLayout == null) {
            return;
        }

        rootLayout.setOnApplyWindowInsetsListener((v, insets) -> {
            int topInset = 0;
            int bottomInset = 0;

            if (insets != null) {
                topInset = insets.getSystemWindowInsetTop();
                bottomInset = insets.getSystemWindowInsetBottom();
            }

            v.setPadding(v.getPaddingLeft(), topInset, v.getPaddingRight(), 0);

            if (adViewContainer != null) {
                adViewContainer.setPadding(
                        adViewContainer.getPaddingLeft(),
                        adViewContainer.getPaddingTop(),
                        adViewContainer.getPaddingRight(),
                        dp(4) + bottomInset
                );
            }

            return insets;
        });

        rootLayout.requestApplyInsets();
    }

    private void setupKeyboardListener() {
        // Nie używamy już ręcznego przesuwania dolnego paska.
        // Pole odpowiedzi wróciło do treści quizu, tuż pod zadaniem.
    }

    private void resetAnswerBarPosition() {
        if (bottomAnswerBar != null) {
            bottomAnswerBar.setTranslationY(0f);
        }
    }

    private void showRulesDialog() {
        String title;
        String message;

        if (currentMode == MODE_LEARNING) {
            title = "Zasady zaliczenia kategorii";
            message = buildLearningRulesText();
        } else if (currentMode == MODE_TEST) {
            title = "Zasady testu 20 pytań";
            message = buildTestRulesText();
        } else if (currentMode == MODE_DAILY) {
            title = "Zasady dziennego wyzwania";
            message = buildDailyRulesText();
        } else {
            title = "Zasady";
            message = "Wybierz kategorię, test albo dzienne wyzwanie.";
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Rozumiem", null)
                .show();
    }

    private String buildLearningRulesText() {
        String tableText = selectedTable > 0 ? " przez " + selectedTable : "";

        return "Cel: zalicz całą kategorię mnożenia" + tableText + ".\n\n" +
                "1. W kategorii jest 10 zadań, od 1×" + selectedTable + " do 10×" + selectedTable + ".\n" +
                "2. Zadania pojawiają się seriami. W jednej serii każde nieopanowane zadanie pojawia się jeden raz.\n" +
                "3. Zadanie jest opanowane dopiero wtedy, gdy odpowiesz dobrze 5 razy z rzędu.\n" +
                "4. Każdy błąd resetuje licznik tego zadania do zera.\n" +
                "5. Gdy opanujesz wszystkie 10 zadań, kategoria zostaje zaliczona i zapisuje się data.\n\n" +
                "W skrócie: 5 dobrych odpowiedzi z rzędu dla każdego zadania.";
    }

    private String buildTestRulesText() {
        return "Test ma 20 losowych pytań z całej tabliczki mnożenia od 1×1 do 10×10.\n\n" +
                "Zasady wyniku czasowego:\n" +
                "• aplikacja mierzy czas od pierwszego pytania,\n" +
                "• za każdą złą odpowiedź doliczane jest +10 sekund,\n" +
                "• ranking zapisuje najlepsze wyniki,\n" +
                "• końcowy wynik to: czas + kary za błędy.\n\n" +
                "Cel: odpowiedz szybko i bez błędów.";
    }

    private String buildDailyRulesText() {
        return "Dzienne wyzwanie ma 10 pytań na dziś.\n\n" +
                "Zasady:\n" +
                "• pytania są losowane dla danego dnia,\n" +
                "• za wynik 10/10 dostajesz bonus gwiazdkowy,\n" +
                "• bonus dzienny można zdobyć raz dziennie.\n\n" +
                "Cel: wracaj codziennie i utrwalaj tabliczkę mnożenia.";
    }

    private void styleStaticButtons() {
        if (backButton != null) {
            backButton.setBackgroundResource(R.drawable.bg_back_button);
            backButton.setImageResource(R.drawable.ic_back_thick);
            backButton.setPadding(dp(6), dp(6), dp(6), dp(6));
            backButton.setScaleType(ImageView.ScaleType.CENTER);
        }

        if (rulesButton != null) {
            rulesButton.setBackgroundResource(R.drawable.bg_back_button);
            rulesButton.setTextColor(Color.parseColor("#2F3A4A"));
            rulesButton.setTextSize(24);
            rulesButton.setTypeface(null, android.graphics.Typeface.BOLD);
            rulesButton.setAllCaps(false);
            rulesButton.setOnClickListener(v -> showRulesDialog());
        }

        styleActionButton(checkButton, R.drawable.bg_action_green, 18, Color.WHITE);
        styleActionButton(nextButton, R.drawable.bg_action_blue, 18, Color.WHITE);
        styleActionButton(retakeButton, R.drawable.bg_action_green, 17, Color.WHITE);
    }

    private void styleActionButton(Button button, int backgroundRes, int textSizeSp, int textColor) {
        if (button == null) {
            return;
        }

        button.setBackgroundResource(backgroundRes);
        button.setTextColor(textColor);
        button.setTextSize(textSizeSp);
        button.setAllCaps(false);
        button.setPadding(dp(14), dp(10), dp(14), dp(10));
    }

    private void setupAds() {
        RequestConfiguration requestConfiguration = new RequestConfiguration.Builder()
                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
                .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build();

        MobileAds.setRequestConfiguration(requestConfiguration);

        new Thread(() -> MobileAds.initialize(
                MainActivity.this,
                initializationStatus -> runOnUiThread(() -> {
                    loadBannerAd();
                    loadInterstitialAd();
                })
        )).start();
    }

    private void loadBannerAd() {
        if (adViewContainer == null) {
            return;
        }

        adViewContainer.post(() -> {
            int adWidth = getAdWidthInDp();

            if (adWidth <= 0) {
                adWidth = 360;
            }

            if (adView != null) {
                adView.destroy();
                adViewContainer.removeAllViews();
            }

            adView = new AdView(MainActivity.this);
            adView.setAdUnitId(getString(R.string.admob_banner_id));
            adView.setAdSize(AdSize.getLargeAnchoredAdaptiveBannerAdSize(MainActivity.this, adWidth));

            adViewContainer.addView(adView);
            adView.loadAd(new AdRequest.Builder().build());
        });
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(
                this,
                getString(R.string.admob_interstitial_id),
                adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        interstitialAd = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        interstitialAd = null;
                    }
                }
        );
    }

    private void showInterstitialIfAvailable() {
        if (interstitialAd == null) {
            loadInterstitialAd();
            return;
        }

        InterstitialAd adToShow = interstitialAd;
        interstitialAd = null;

        adToShow.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                loadInterstitialAd();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                loadInterstitialAd();
            }
        });

        adToShow.show(this);
    }

    private int getAdWidthInDp() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int adWidthPixels = adViewContainer.getWidth();

        if (adWidthPixels == 0) {
            adWidthPixels = displayMetrics.widthPixels;
        }

        return Math.round(adWidthPixels / displayMetrics.density);
    }

    @Override
    protected void onPause() {
        if (adView != null) {
            adView.pause();
        }

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (adView != null) {
            adView.destroy();
            adView = null;
        }

        super.onDestroy();
    }

    private void createMenuButtons() {
        menuContainer.removeAllViews();

        LinearLayout currentRow = null;

        for (int table = MIN_TABLE; table <= MAX_TABLE; table++) {
            if ((table - 1) % 2 == 0) {
                currentRow = createTilesRow();
                menuContainer.addView(currentRow);
            }

            LinearLayout tile = createCategoryTile(table);
            if (currentRow != null) {
                currentRow.addView(tile);
            }
        }

        LinearLayout testRow = createTilesRow();
        testRow.addView(createTestTile());
        testRow.addView(createDailyTile());
        menuContainer.addView(testRow);
    }

    private LinearLayout createTilesRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return row;
    }

    private LinearLayout createCategoryTile(int table) {
        LinearLayout tile = createTileContainer();

        Button button = createMenuButton(
                "Mnożenie\nprzez " + table,
                R.drawable.bg_category_button
        );

        TextView status = createStatusTextView();
        final int chosenTable = table;
        button.setOnClickListener(v -> startLearningQuiz(chosenTable));

        tableButtons[table] = button;
        statusTextViews[table] = status;

        tile.addView(button);
        tile.addView(status);
        return tile;
    }

    private LinearLayout createTestTile() {
        LinearLayout tile = createTileContainer();

        Button button = createMenuButton("TEST\n20 pytań", R.drawable.bg_test_button);
        TextView status = createStatusTextView();

        button.setOnClickListener(v -> startTest());

        testStatusTextView = status;

        tile.addView(button);
        tile.addView(status);
        return tile;
    }

    private LinearLayout createDailyTile() {
        LinearLayout tile = createTileContainer();

        Button button = createMenuButton("WYZWANIE\nDZIŚ", R.drawable.bg_daily_button);
        TextView status = createStatusTextView();

        button.setOnClickListener(v -> startDailyChallenge());

        dailyStatusTextView = status;

        tile.addView(button);
        tile.addView(status);
        return tile;
    }

    private LinearLayout createTileContainer() {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        params.setMargins(dp(4), dp(4), dp(4), dp(4));
        tile.setLayoutParams(params);

        return tile;
    }

    private Button createMenuButton(String text, int backgroundRes) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(getMenuButtonTextSize());
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setMinHeight(dp(60));
        button.setPadding(dp(10), dp(10), dp(10), dp(10));
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(false);
        button.setBackgroundResource(backgroundRes);
        button.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return button;
    }

    private TextView createStatusTextView() {
        TextView status = new TextView(this);
        status.setTextSize(12);
        status.setGravity(Gravity.CENTER);
        status.setSingleLine(false);
        status.setPadding(dp(4), dp(4), dp(4), dp(0));
        status.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return status;
    }

    private int getMenuButtonTextSize() {
        float heightDp = getResources().getDisplayMetrics().heightPixels / getResources().getDisplayMetrics().density;
        return heightDp < 700 ? 14 : 16;
    }

    private void setupQuizButtons() {
        backButton.setOnClickListener(v -> showMenu());

        retakeButton.setOnClickListener(v -> startRetakeLearningQuiz());

        checkButton.setOnClickListener(v -> checkAnswer());

        answerEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                checkAnswer();
                return true;
            }
            return false;
        });
    }

    private void showMenuHeader() {
        if (titleTextView != null) {
            titleTextView.setVisibility(View.VISIBLE);
        }
        subtitleTextView.setVisibility(View.VISIBLE);
        starsTextView.setVisibility(View.VISIBLE);

        String badges = buildBadgesText();
        if (badges.isEmpty()) {
            badgesTextView.setVisibility(View.GONE);
        } else {
            badgesTextView.setVisibility(View.VISIBLE);
        }

        if (robotTextView != null) {
            robotTextView.setVisibility(View.GONE);
        }
    }

    private void hideMenuHeaderForQuiz() {
        if (titleTextView != null) {
            titleTextView.setVisibility(View.GONE);
        }
        subtitleTextView.setVisibility(View.GONE);
        starsTextView.setVisibility(View.GONE);
        badgesTextView.setVisibility(View.GONE);

        if (robotTextView != null) {
            robotTextView.setVisibility(View.GONE);
        }
    }

    private void showMenu() {
        stopTestTimer();
        currentMode = MODE_MENU;
        selectedTable = 0;
        currentMultiplier = 0;
        currentSeriesIndex = 0;
        currentLeftNumber = 0;
        currentRightNumber = 0;
        currentTestIndex = 0;
        currentTestCorrectAnswers = 0;
        currentLearningRetake = false;
        currentSeries.clear();
        testQuestions.clear();

        hideAnswerBar();
        hideRetakeButton();
        showMenuHeader();
        menuContainer.setVisibility(View.VISIBLE);
        quizContainer.setVisibility(View.GONE);

        showBannerAd();
        refreshMenuStatuses();
        updateRewardsHeader();
    }

    private void refreshMenuStatuses() {
        for (int table = MIN_TABLE; table <= MAX_TABLE; table++) {
            saveCompletionIfNeeded(table);

            TextView status = statusTextViews[table];
            if (status == null) {
                continue;
            }

            if (isCompleted(table)) {
                int count = getCompletionCount(table);
                String date = getCompletionDate(table);
                status.setText("🏆×" + count + "\nostatnio\n" + date);
                status.setTextColor(Color.rgb(0, 128, 0));
            } else {
                int learned = countLearnedTasks(table);
                status.setText(learned + "/10 nauczone");
                status.setTextColor(Color.DKGRAY);
            }
        }

        refreshTestStatus();
        refreshDailyStatus();
    }

    private void refreshTestStatus() {
        if (testStatusTextView == null) {
            return;
        }

        int bestScore = prefs.getInt(keyTestBestScore(), -1);
        int lastScore = prefs.getInt(keyTestLastScore(), -1);
        String lastDate = prefs.getString(keyTestLastDate(), "");
        int bestTime = prefs.getInt(keyTestBestTimeSeconds(), -1);
        int lastFinalSeconds = prefs.getInt(keyTestLastFinalSeconds(), -1);
        String completedDate = prefs.getString(keyTestCompletedDate(), "");

        if (bestTime >= 0) {
            testStatusTextView.setText("🏆 Rekord\n" + formatSeconds(bestTime) +
                    "\nostatnio " + (lastFinalSeconds >= 0 ? formatSeconds(lastFinalSeconds) : "-"));
            testStatusTextView.setTextColor(Color.rgb(0, 128, 0));
        } else if (!completedDate.isEmpty()) {
            testStatusTextView.setText("✅ Test zaliczony\n" + completedDate);
            testStatusTextView.setTextColor(Color.rgb(0, 128, 0));
        } else if (lastScore >= 0) {
            testStatusTextView.setText("Ostatnio: " + lastScore + "/20\n" + lastDate);
            testStatusTextView.setTextColor(Color.DKGRAY);
        } else if (bestScore >= 0) {
            testStatusTextView.setText("Rekord\n" + bestScore + "/20");
            testStatusTextView.setTextColor(Color.DKGRAY);
        } else {
            testStatusTextView.setText("20 pytań\nranking czasu");
            testStatusTextView.setTextColor(Color.DKGRAY);
        }
    }

    private void refreshDailyStatus() {
        if (dailyStatusTextView == null) {
            return;
        }

        String today = getToday();
        String completedDate = prefs.getString(keyDailyCompletedDate(), "");
        String lastDate = prefs.getString(keyDailyLastDate(), "");
        int lastScore = prefs.getInt(keyDailyLastScore(), -1);

        if (today.equals(completedDate)) {
            dailyStatusTextView.setText("🔥 Zrobione dziś\n10/10");
            dailyStatusTextView.setTextColor(Color.rgb(0, 128, 0));
        } else if (today.equals(lastDate) && lastScore >= 0) {
            dailyStatusTextView.setText("Dziś: " + lastScore + "/10\nspróbuj 10/10");
            dailyStatusTextView.setTextColor(Color.DKGRAY);
        } else {
            dailyStatusTextView.setText("10 pytań\nbonus ⭐");
            dailyStatusTextView.setTextColor(Color.DKGRAY);
        }
    }

    private void updateRewardsHeader() {
        int stars = getStars();
        int badgeCount = countBadges();

        starsTextView.setText("⭐ Gwiazdki: " + stars + "    🏅 Odznaki: " + badgeCount);

        String badges = buildBadgesText();
        if (badges.isEmpty()) {
            badgesTextView.setText("");
            badgesTextView.setVisibility(View.GONE);
        } else {
            badgesTextView.setVisibility(View.VISIBLE);
            badgesTextView.setText(badges);
        }

        if (robotTextView != null) {
            if (badgeCount <= 0) {
                robotTextView.setText("🤖 RoboMat: Cześć! Trenuj codziennie i zbieraj odznaki.");
            } else {
                robotTextView.setText("🤖 RoboMat: Masz już " + badgeCount + " odznak. Tak trzymaj!");
            }
        }

        if (lastBadgeCount >= 0 && badgeCount > lastBadgeCount) {
            animateBadgeUnlock();
            if (robotTextView != null) {
                robotTextView.setText("🤖 RoboMat: Nowa odznaka! Świetna robota!");
            }
        }

        lastBadgeCount = badgeCount;
    }

    private void animateBadgeUnlock() {
        if (badgesTextView != null) {
            badgesTextView.animate().cancel();
            badgesTextView.setScaleX(0.92f);
            badgesTextView.setScaleY(0.92f);
            badgesTextView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(420)
                    .setInterpolator(new OvershootInterpolator())
                    .start();
        }

        if (robotTextView != null) {
            robotTextView.animate().cancel();
            robotTextView.setRotation(-5f);
            robotTextView.animate()
                    .rotation(0f)
                    .setDuration(420)
                    .setInterpolator(new OvershootInterpolator())
                    .start();
        }
    }

    private String buildBadgesText() {
        ArrayList<String> badges = new ArrayList<>();

        for (int table = MIN_TABLE; table <= MAX_TABLE; table++) {
            if (isCompleted(table)) {
                badges.add("🏆×" + table);
            }
        }

        if (!prefs.getString(keyTestCompletedDate(), "").isEmpty()) {
            badges.add("🎯 Test");
        }

        if (getStars() >= 50) {
            badges.add("⭐50");
        }

        if (getStars() >= 100) {
            badges.add("⭐100");
        }

        if (getStars() >= 250) {
            badges.add("⭐250");
        }

        if (getDailyCompletedCount() >= 3) {
            badges.add("🔥3 dni");
        }

        if (getDailyCompletedCount() >= 7) {
            badges.add("🔥7 dni");
        }

        if (badges.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder("Twoje odznaki: ");
        for (int i = 0; i < badges.size(); i++) {
            if (i > 0) {
                builder.append("  ");
            }
            builder.append(badges.get(i));
        }

        return builder.toString();
    }

    private int countBadges() {
        int count = 0;

        for (int table = MIN_TABLE; table <= MAX_TABLE; table++) {
            if (isCompleted(table)) {
                count++;
            }
        }

        if (!prefs.getString(keyTestCompletedDate(), "").isEmpty()) {
            count++;
        }

        if (getStars() >= 50) {
            count++;
        }

        if (getStars() >= 100) {
            count++;
        }

        if (getStars() >= 250) {
            count++;
        }

        if (getDailyCompletedCount() >= 3) {
            count++;
        }

        if (getDailyCompletedCount() >= 7) {
            count++;
        }

        return count;
    }

    private void startLearningQuiz(int table) {
        currentMode = MODE_LEARNING;
        currentLearningRetake = false;
        selectedTable = table;
        currentMultiplier = 0;
        currentSeriesIndex = 0;
        currentLeftNumber = 0;
        currentRightNumber = 0;
        currentSeries.clear();
        testQuestions.clear();

        hideMenuHeaderForQuiz();
        menuContainer.setVisibility(View.GONE);
        quizContainer.setVisibility(View.VISIBLE);

        quizTitleTextView.setText("Mnożenie przez " + table);

        if (isCompleted(table)) {
            showCompletedLearningScreen();
        } else {
            startNewLearningSeries();
        }
    }

    private void showCompletedLearningScreen() {
        showBannerAd();
        currentLearningRetake = false;

        int count = getCompletionCount(selectedTable);
        String datesText = buildCompletionDatesText(selectedTable);

        progressTextView.setText("Ta kategoria była zaliczona " + count + " razy.");
        questionTextView.setText("🏆 Zaliczone ×" + count);
        resultTextView.setText("Historia zaliczeń:\n" + datesText);
        resultTextView.setTextColor(Color.rgb(0, 128, 0));
        setRobotFaceHappy();

        hideAnswerBar();
        answerEditText.setVisibility(View.GONE);
        checkButton.setVisibility(View.GONE);

        retakeButton.setVisibility(View.VISIBLE);
        retakeButton.setEnabled(true);
        retakeButton.setText("Zalicz jeszcze raz");
        retakeButton.setOnClickListener(v -> startRetakeLearningQuiz());

        nextButton.setVisibility(View.VISIBLE);
        nextButton.setText("Wróć do menu");
        nextButton.setOnClickListener(v -> showMenu());
    }

    private void startRetakeLearningQuiz() {
        currentLearningRetake = true;
        resetTableProgress(selectedTable);

        hideRetakeButton();
        hideBannerAd();

        progressTextView.setText("Ponowne zaliczanie kategorii.");
        questionTextView.setText("");
        resultTextView.setText("");
        resultTextView.setTextColor(Color.DKGRAY);

        if (bottomAnswerBar != null) {
            bottomAnswerBar.setVisibility(View.GONE);
        }

        answerEditText.setVisibility(View.VISIBLE);
        checkButton.setVisibility(View.VISIBLE);
        checkButton.setEnabled(true);
        checkButton.setText("Zatwierdź");
        checkButton.setOnClickListener(v -> checkAnswer());

        nextButton.setVisibility(View.GONE);

        startNewLearningSeries();
    }

    private void startNewLearningSeries() {
        currentSeries.clear();
        currentSeriesIndex = 0;

        for (int multiplier = MIN_MULTIPLIER; multiplier <= MAX_MULTIPLIER; multiplier++) {
            if (getStreak(selectedTable, multiplier) < REQUIRED_STREAK) {
                currentSeries.add(multiplier);
            }
        }

        if (currentSeries.isEmpty()) {
            recordCategoryCompletion(selectedTable);
            currentLearningRetake = false;
            showCompletedLearningScreen();
            return;
        }

        Collections.shuffle(currentSeries, random);
        showCurrentLearningQuestionFromSeries();
    }

    private void showCurrentLearningQuestionFromSeries() {
        if (currentSeries.isEmpty()) {
            startNewLearningSeries();
            return;
        }

        if (currentSeriesIndex >= currentSeries.size()) {
            startNewLearningSeries();
            return;
        }

        currentMultiplier = currentSeries.get(currentSeriesIndex);
        currentLeftNumber = currentMultiplier;
        currentRightNumber = selectedTable;

        prepareQuestionScreen();
        questionTextView.setText(currentLeftNumber + " × " + currentRightNumber + " = ?");
        updateLearningProgressText();
    }

    private void updateLearningProgressText() {
        int learned = countLearnedTasks(selectedTable);
        int streak = getStreak(selectedTable, currentMultiplier);

        String seriesInfo = "Seria: " + (currentSeriesIndex + 1) + "/" + currentSeries.size();

        progressTextView.setText(
                seriesInfo + "\n" +
                        "Nauczone zadania: " + learned + "/10\n" +
                        "To zadanie: " + streak + "/" + REQUIRED_STREAK + " dobrych odpowiedzi pod rząd"
        );
    }
    private void startTest() {
        currentMode = MODE_TEST;
        resetQuestionState();

        hideMenuHeaderForQuiz();
        menuContainer.setVisibility(View.GONE);
        quizContainer.setVisibility(View.VISIBLE);

        quizTitleTextView.setText("Test: 20 losowych zadań");

        hideAnswerBar();
        hideRetakeButton();
        hideKeyboard();
        showBannerAd();

        questionTextView.setText("Gotowy?");
        progressTextView.setText(
                "Test 20 pytań\n" +
                        "Czas zacznie się liczyć dopiero po kliknięciu Start."
        );

        resultTextView.setText(
                "Zasady:\n" +
                        "• 20 losowych zadań od 1×1 do 10×10\n" +
                        "• za każdą złą odpowiedź doliczamy +10 sekund\n" +
                        "• wynik zapisze się w rankingu"
        );
        resultTextView.setTextColor(Color.DKGRAY);
        setRobotFaceNeutral();

        answerEditText.setVisibility(View.GONE);
        checkButton.setVisibility(View.GONE);

        nextButton.setVisibility(View.VISIBLE);
        nextButton.setText("Start test");
        nextButton.setOnClickListener(v -> startTimedTestQuestions());
    }

    private void startTimedTestQuestions() {
        currentTestIndex = 0;
        currentTestCorrectAnswers = 0;
        testWrongAnswers = 0;
        testQuestions.clear();

        startTestTimer();
        createRandomTestQuestions(TEST_QUESTION_COUNT, random);
        showCurrentTestQuestion();
    }

    private void startDailyChallenge() {
        currentMode = MODE_DAILY;
        resetQuestionState();

        hideMenuHeaderForQuiz();
        menuContainer.setVisibility(View.GONE);
        quizContainer.setVisibility(View.VISIBLE);

        quizTitleTextView.setText("Dzienne wyzwanie");

        createRandomTestQuestions(DAILY_QUESTION_COUNT, new Random(getToday().hashCode()));
        showCurrentTestQuestion();
    }

    private void resetQuestionState() {
        hideRetakeButton();
        stopTestTimer();
        selectedTable = 0;
        currentMultiplier = 0;
        currentSeriesIndex = 0;
        currentTestIndex = 0;
        currentTestCorrectAnswers = 0;
        testWrongAnswers = 0;
        testStartTimeMillis = 0L;
        currentSeries.clear();
        testQuestions.clear();
    }

    private void createRandomTestQuestions(int questionCount, Random sourceRandom) {
        ArrayList<Question> allQuestions = new ArrayList<>();

        for (int left = TEST_MIN_NUMBER; left <= TEST_MAX_NUMBER; left++) {
            for (int right = TEST_MIN_NUMBER; right <= TEST_MAX_NUMBER; right++) {
                allQuestions.add(new Question(left, right));
            }
        }

        Collections.shuffle(allQuestions, sourceRandom);

        for (int i = 0; i < questionCount && i < allQuestions.size(); i++) {
            testQuestions.add(allQuestions.get(i));
        }
    }

    private void showCurrentTestQuestion() {
        if (currentTestIndex >= testQuestions.size()) {
            finishCurrentTestMode();
            return;
        }

        Question question = testQuestions.get(currentTestIndex);
        currentLeftNumber = question.left;
        currentRightNumber = question.right;

        prepareQuestionScreen();
        questionTextView.setText(currentLeftNumber + " × " + currentRightNumber + " = ?");
        updateTestProgressText();
    }

    private void updateTestProgressText() {
        String label = currentMode == MODE_DAILY ? "Dzienne wyzwanie" : "Test";
        int total = currentMode == MODE_DAILY ? DAILY_QUESTION_COUNT : TEST_QUESTION_COUNT;

        String timerLine = "";

        if (currentMode == MODE_TEST) {
            long rawSeconds = getCurrentTestRawSeconds();
            int penaltySeconds = testWrongAnswers * 10;
            int totalSeconds = (int) rawSeconds + penaltySeconds;

            timerLine = "⏱ Czas: " + formatSeconds((int) rawSeconds) +
                    " + kara " + penaltySeconds + "s = " + formatSeconds(totalSeconds) + "\n";
        }

        progressTextView.setText(
                label + "\n" +
                        timerLine +
                        "Pytanie: " + (currentTestIndex + 1) + "/" + testQuestions.size() + "\n" +
                        "Dobrych odpowiedzi: " + currentTestCorrectAnswers + "/" + total
        );
    }

    private void prepareQuestionScreen() {
        hideRetakeButton();
        hideBannerAd();
        showAnswerBar();

        answerEditText.setVisibility(View.VISIBLE);
        checkButton.setVisibility(View.VISIBLE);

        answerEditText.setText("");
        answerEditText.setEnabled(true);
        answerEditText.requestFocus();

        checkButton.setEnabled(true);
        checkButton.setText("Zatwierdź");
        checkButton.setOnClickListener(v -> checkAnswer());

        nextButton.setVisibility(View.GONE);
        nextButton.setText("Następne pytanie");

        resultTextView.setText("");
        resultTextView.setTextColor(Color.DKGRAY);
        setRobotFaceNeutral();

        answerEditText.postDelayed(this::showKeyboardAndKeepInputVisible, 180);
    }

    private void checkAnswer() {
        if (currentMode == MODE_LEARNING) {
            checkLearningAnswer();
        } else if (currentMode == MODE_TEST || currentMode == MODE_DAILY) {
            checkTestAnswer();
        }
    }

    private Integer readUserAnswer() {
        String answerText = answerEditText.getText().toString().trim();

        if (answerText.isEmpty()) {
            resultTextView.setText("❌ Wpisz odpowiedź.");
            resultTextView.setTextColor(Color.rgb(190, 0, 0));
            return null;
        }

        try {
            return Integer.parseInt(answerText);
        } catch (NumberFormatException e) {
            resultTextView.setText("❌ To nie wygląda jak liczba. Wpisz sam wynik, np. 25.");
            resultTextView.setTextColor(Color.rgb(190, 0, 0));
            return null;
        }
    }

    private void checkLearningAnswer() {
        if (selectedTable == 0 || currentMultiplier == 0) {
            return;
        }

        Integer userAnswerObject = readUserAnswer();
        if (userAnswerObject == null) {
            return;
        }

        int userAnswer = userAnswerObject;
        int correctAnswer = currentLeftNumber * currentRightNumber;
        hideKeyboard();
        hideAnswerBar();
        showBannerAd();

        if (userAnswer == correctAnswer) {
            int oldStreak = getStreak(selectedTable, currentMultiplier);
            int newStreak = Math.min(REQUIRED_STREAK, oldStreak + 1);
            saveStreak(selectedTable, currentMultiplier, newStreak);

            int earnedStars = 1;
            String bonusText = "";

            if (oldStreak < REQUIRED_STREAK && newStreak >= REQUIRED_STREAK) {
                earnedStars += 5;
                bonusText = "\n⭐ To zadanie jest opanowane! Bonus +5 gwiazdek.";
            }

            addStars(earnedStars);

            resultTextView.setText(
                    "✅ " + getRandomHappyMessage() + " " + currentLeftNumber + "×" + currentRightNumber +
                            " to " + correctAnswer + ".\n" +
                            "⭐ +" + earnedStars + " gwiazdek." + bonusText
            );
            resultTextView.setTextColor(Color.rgb(0, 128, 0));
            setRobotFaceHappy();
        } else {
            saveStreak(selectedTable, currentMultiplier, 0);

            resultTextView.setText(
                    "❌ Źle, " + currentLeftNumber + "×" + currentRightNumber +
                            " to " + correctAnswer + ", a nie jak podałeś " + userAnswer + ".\n" +
                            getRandomTryAgainMessage()
            );
            resultTextView.setTextColor(Color.rgb(190, 0, 0));
            setRobotFaceSad();
        }

        updateLearningProgressText();
        answerEditText.setEnabled(false);
        checkButton.setEnabled(false);

        if (isTableFullyLearned(selectedTable)) {
            recordCategoryCompletion(selectedTable);
            currentLearningRetake = false;
            String date = getCompletionDate(selectedTable);
            int count = getCompletionCount(selectedTable);

            resultTextView.setText(
                    resultTextView.getText().toString() +
                            "\n\n🏆 Cała kategoria zaliczona dnia " + date + "!\n" +
                            "To jest zaliczenie numer " + count + "."
            );

            nextButton.setText("Zobacz zaliczenia");
            nextButton.setOnClickListener(v -> showCompletedLearningScreen());
            showInterstitialIfAvailable();
        } else if (isLastQuestionInCurrentLearningSeries()) {
            nextButton.setText("Rozpocznij kolejną serię");
            nextButton.setOnClickListener(v -> startNewLearningSeries());
        } else {
            nextButton.setText("Następne pytanie");
            nextButton.setOnClickListener(v -> {
                currentSeriesIndex++;
                showCurrentLearningQuestionFromSeries();
            });
        }

        nextButton.setVisibility(View.VISIBLE);
    }

    private void checkTestAnswer() {
        if (currentTestIndex >= testQuestions.size()) {
            return;
        }

        Integer userAnswerObject = readUserAnswer();
        if (userAnswerObject == null) {
            return;
        }

        int userAnswer = userAnswerObject;
        int correctAnswer = currentLeftNumber * currentRightNumber;
        hideKeyboard();
        hideAnswerBar();
        showBannerAd();

        if (userAnswer == correctAnswer) {
            currentTestCorrectAnswers++;
            addStars(1);
            resultTextView.setText(
                    "✅ " + getRandomHappyMessage() + " " + currentLeftNumber + "×" + currentRightNumber +
                            " to " + correctAnswer + ".\n⭐ +1 gwiazdka."
            );
            resultTextView.setTextColor(Color.rgb(0, 128, 0));
            setRobotFaceHappy();
        } else {
            if (currentMode == MODE_TEST) {
                testWrongAnswers++;
            }

            resultTextView.setText(
                    "❌ Źle, " + currentLeftNumber + "×" + currentRightNumber +
                            " to " + correctAnswer + ", a nie jak podałeś " + userAnswer + ".\n" +
                            getRandomTryAgainMessage() +
                            (currentMode == MODE_TEST ? "\n⏱ Kara do wyniku testu: +10 sekund." : "")
            );
            resultTextView.setTextColor(Color.rgb(190, 0, 0));
            setRobotFaceSad();
        }

        answerEditText.setEnabled(false);
        checkButton.setEnabled(false);
        updateTestProgressText();

        if (currentTestIndex >= testQuestions.size() - 1) {
            nextButton.setText("Pokaż wynik");
            nextButton.setOnClickListener(v -> finishCurrentTestMode());
        } else {
            nextButton.setText("Następne pytanie");
            nextButton.setOnClickListener(v -> {
                currentTestIndex++;
                showCurrentTestQuestion();
            });
        }

        nextButton.setVisibility(View.VISIBLE);
    }

    private void finishCurrentTestMode() {
        if (currentMode == MODE_DAILY) {
            finishDailyChallenge();
        } else {
            finishTest();
        }
    }

    private void finishTest() {
        currentMode = MODE_TEST;
        hideKeyboard();
        showBannerAd();

        int rawSeconds = (int) getCurrentTestRawSeconds();
        int penaltySeconds = testWrongAnswers * 10;
        int finalSeconds = rawSeconds + penaltySeconds;

        stopTestTimer();

        boolean firstPerfect = currentTestCorrectAnswers == TEST_QUESTION_COUNT &&
                prefs.getString(keyTestCompletedDate(), "").isEmpty();

        saveTestResult(currentTestCorrectAnswers, rawSeconds, penaltySeconds, finalSeconds, testWrongAnswers);

        hideAnswerBar();
        answerEditText.setVisibility(View.GONE);
        checkButton.setVisibility(View.GONE);

        questionTextView.setText(currentTestCorrectAnswers + "/" + TEST_QUESTION_COUNT);
        progressTextView.setText(
                "Test zakończony.\n" +
                        "Czas: " + formatSeconds(rawSeconds) +
                        " + kara " + penaltySeconds + "s = " + formatSeconds(finalSeconds)
        );

        String rankingText = buildTestRankingText();

        if (currentTestCorrectAnswers == TEST_QUESTION_COUNT) {
            String date = prefs.getString(keyTestCompletedDate(), "");
            String bonus = "";

            if (firstPerfect) {
                addStars(20);
                bonus = "\n🎁 Pierwszy perfekcyjny test: +20 gwiazdek!";
            }

            resultTextView.setText(
                    "🏆 Perfekcyjnie! Test zaliczony dnia " + date + ".\n" +
                            "Wynik: " + currentTestCorrectAnswers + "/" + TEST_QUESTION_COUNT + ".\n" +
                            "⏱ Wynik czasowy: " + formatSeconds(finalSeconds) +
                            " (błędy: " + testWrongAnswers + ")" + bonus +
                            "\n\n" + rankingText
            );
            resultTextView.setTextColor(Color.rgb(0, 128, 0));
            setRobotFaceHappy();
        } else {
            resultTextView.setText(
                    "💪 Koniec testu.\n" +
                            "Wynik: " + currentTestCorrectAnswers + "/" + TEST_QUESTION_COUNT + ".\n" +
                            "⏱ Wynik czasowy: " + formatSeconds(finalSeconds) +
                            " (czas " + formatSeconds(rawSeconds) + " + kara " + penaltySeconds + "s)." +
                            "\n\n" + rankingText
            );
            resultTextView.setTextColor(Color.rgb(190, 0, 0));
            setRobotFaceSad();
        }

        nextButton.setVisibility(View.VISIBLE);
        nextButton.setText("Wróć do menu");
        nextButton.setOnClickListener(v -> showMenu());

        showInterstitialIfAvailable();
    }

    private void finishDailyChallenge() {
        currentMode = MODE_DAILY;
        hideKeyboard();
        showBannerAd();

        String today = getToday();
        boolean firstPerfectToday = currentTestCorrectAnswers == DAILY_QUESTION_COUNT &&
                !today.equals(prefs.getString(keyDailyCompletedDate(), ""));

        saveDailyResult(currentTestCorrectAnswers);

        hideAnswerBar();
        answerEditText.setVisibility(View.GONE);
        checkButton.setVisibility(View.GONE);

        questionTextView.setText(currentTestCorrectAnswers + "/" + DAILY_QUESTION_COUNT);
        progressTextView.setText("Dzienne wyzwanie zakończone.");

        if (currentTestCorrectAnswers == DAILY_QUESTION_COUNT) {
            String bonus = "";

            if (firstPerfectToday) {
                addStars(15);
                increaseDailyCompletedCount();
                bonus = "\n🎁 Bonus dzienny: +15 gwiazdek!";
            }

            resultTextView.setText(
                    "🔥 Dzienne wyzwanie zaliczone!\n" +
                            "Wynik: " + currentTestCorrectAnswers + "/" + DAILY_QUESTION_COUNT + "." + bonus
            );
            resultTextView.setTextColor(Color.rgb(0, 128, 0));
            setRobotFaceHappy();
        } else {
            resultTextView.setText(
                    "💪 Dzisiaj: " + currentTestCorrectAnswers + "/" + DAILY_QUESTION_COUNT + ".\n" +
                            "Spróbuj jeszcze raz, żeby zgarnąć dzienny bonus."
            );
            resultTextView.setTextColor(Color.rgb(190, 0, 0));
            setRobotFaceSad();
        }

        nextButton.setVisibility(View.VISIBLE);
        nextButton.setText("Wróć do menu");
        nextButton.setOnClickListener(v -> showMenu());

        showInterstitialIfAvailable();
    }

    private void startTestTimer() {
        testStartTimeMillis = System.currentTimeMillis();
        testWrongAnswers = 0;
        testTimerRunning = true;
        timerHandler.removeCallbacksAndMessages(null);
        timerHandler.post(new Runnable() {
            @Override
            public void run() {
                if (testTimerRunning && currentMode == MODE_TEST) {
                    updateTestProgressText();
                    timerHandler.postDelayed(this, 500);
                }
            }
        });
    }

    private void stopTestTimer() {
        testTimerRunning = false;
        timerHandler.removeCallbacksAndMessages(null);
    }

    private long getCurrentTestRawSeconds() {
        if (testStartTimeMillis <= 0L) {
            return 0L;
        }

        long millis = System.currentTimeMillis() - testStartTimeMillis;
        return Math.max(0L, (millis + 999L) / 1000L);
    }

    private String formatSeconds(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    private String addTestRankingEntry(
            String oldRanking,
            String date,
            int score,
            int rawSeconds,
            int penaltySeconds,
            int finalSeconds,
            int wrongAnswers
    ) {
        ArrayList<TestResult> results = parseTestRanking(oldRanking);
        results.add(new TestResult(date, score, rawSeconds, penaltySeconds, finalSeconds, wrongAnswers));

        Collections.sort(results, (a, b) -> {
            if (a.finalSeconds != b.finalSeconds) {
                return a.finalSeconds - b.finalSeconds;
            }

            if (a.wrongAnswers != b.wrongAnswers) {
                return a.wrongAnswers - b.wrongAnswers;
            }

            return b.score - a.score;
        });

        StringBuilder builder = new StringBuilder();

        int limit = Math.min(10, results.size());

        for (int i = 0; i < limit; i++) {
            TestResult result = results.get(i);

            if (i > 0) {
                builder.append("\n");
            }

            builder.append(result.date).append("|")
                    .append(result.score).append("|")
                    .append(result.rawSeconds).append("|")
                    .append(result.penaltySeconds).append("|")
                    .append(result.finalSeconds).append("|")
                    .append(result.wrongAnswers);
        }

        return builder.toString();
    }

    private ArrayList<TestResult> parseTestRanking(String rankingText) {
        ArrayList<TestResult> results = new ArrayList<>();

        if (rankingText == null || rankingText.trim().isEmpty()) {
            return results;
        }

        String[] lines = rankingText.split("\\n");

        for (String line : lines) {
            String[] parts = line.split("\\|");

            if (parts.length != 6) {
                continue;
            }

            try {
                results.add(new TestResult(
                        parts[0],
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]),
                        Integer.parseInt(parts[3]),
                        Integer.parseInt(parts[4]),
                        Integer.parseInt(parts[5])
                ));
            } catch (NumberFormatException ignored) {
                // Pomijamy uszkodzony wpis rankingu.
            }
        }

        return results;
    }

    private String buildTestRankingText() {
        ArrayList<TestResult> results = parseTestRanking(prefs.getString(keyTestRanking(), ""));

        if (results.isEmpty()) {
            return "Ranking: brak zapisanych wyników.";
        }

        StringBuilder builder = new StringBuilder("🏁 Ranking testu:");

        int limit = Math.min(5, results.size());

        for (int i = 0; i < limit; i++) {
            TestResult result = results.get(i);

            builder.append("\n")
                    .append(i + 1)
                    .append(". ")
                    .append(formatSeconds(result.finalSeconds))
                    .append("  |  ")
                    .append(result.score)
                    .append("/")
                    .append(TEST_QUESTION_COUNT)
                    .append("  |  błędy: ")
                    .append(result.wrongAnswers)
                    .append("  |  ")
                    .append(result.date);
        }

        return builder.toString();
    }

    private void saveTestResult(int score, int rawSeconds, int penaltySeconds, int finalSeconds, int wrongAnswers) {
        String today = getToday();
        int bestScore = prefs.getInt(keyTestBestScore(), -1);
        int bestTime = prefs.getInt(keyTestBestTimeSeconds(), -1);

        SharedPreferences.Editor editor = prefs.edit()
                .putInt(keyTestLastScore(), score)
                .putString(keyTestLastDate(), today)
                .putInt(keyTestLastRawSeconds(), rawSeconds)
                .putInt(keyTestLastPenaltySeconds(), penaltySeconds)
                .putInt(keyTestLastFinalSeconds(), finalSeconds)
                .putInt(keyTestLastWrongAnswers(), wrongAnswers);

        if (score > bestScore) {
            editor.putInt(keyTestBestScore(), score);
        }

        if (bestTime < 0 || finalSeconds < bestTime) {
            editor.putInt(keyTestBestTimeSeconds(), finalSeconds);
        }

        if (score == TEST_QUESTION_COUNT && prefs.getString(keyTestCompletedDate(), "").isEmpty()) {
            editor.putString(keyTestCompletedDate(), today);
        }

        editor.putString(
                keyTestRanking(),
                addTestRankingEntry(
                        prefs.getString(keyTestRanking(), ""),
                        today,
                        score,
                        rawSeconds,
                        penaltySeconds,
                        finalSeconds,
                        wrongAnswers
                )
        );

        editor.apply();
    }

    private void saveDailyResult(int score) {
        String today = getToday();
        int bestScore = prefs.getInt(keyDailyBestScore(), -1);

        SharedPreferences.Editor editor = prefs.edit()
                .putInt(keyDailyLastScore(), score)
                .putString(keyDailyLastDate(), today);

        if (score > bestScore) {
            editor.putInt(keyDailyBestScore(), score);
        }

        if (score == DAILY_QUESTION_COUNT) {
            editor.putString(keyDailyCompletedDate(), today);
        }

        editor.apply();
    }

    private boolean isLastQuestionInCurrentLearningSeries() {
        return currentSeriesIndex >= currentSeries.size() - 1;
    }

    private boolean isTableFullyLearned(int table) {
        for (int multiplier = MIN_MULTIPLIER; multiplier <= MAX_MULTIPLIER; multiplier++) {
            if (getStreak(table, multiplier) < REQUIRED_STREAK) {
                return false;
            }
        }
        return true;
    }

    private int countLearnedTasks(int table) {
        int learned = 0;

        for (int multiplier = MIN_MULTIPLIER; multiplier <= MAX_MULTIPLIER; multiplier++) {
            if (getStreak(table, multiplier) >= REQUIRED_STREAK) {
                learned++;
            }
        }

        return learned;
    }

    private void saveCompletionIfNeeded(int table) {
        if (!isTableFullyLearned(table)) {
            return;
        }

        if (isCompleted(table)) {
            return;
        }

        recordCategoryCompletion(table);
    }

    private void recordCategoryCompletion(int table) {
        String today = getToday();
        String existingDates = getCompletionDatesRaw(table);

        if (existingDates.isEmpty()) {
            String oldDate = prefs.getString(keyCompletedDate(table), "");
            if (!oldDate.isEmpty() && prefs.getBoolean(keyCompleted(table), false)) {
                existingDates = oldDate;
            }
        }

        String newDates;

        if (existingDates.isEmpty()) {
            newDates = today;
        } else {
            newDates = existingDates + "|" + today;
        }

        prefs.edit()
                .putBoolean(keyCompleted(table), true)
                .putString(keyCompletedDate(table), today)
                .putString(keyCompletedDates(table), newDates)
                .apply();
    }

    private boolean isCompleted(int table) {
        return prefs.getBoolean(keyCompleted(table), false) || getCompletionCount(table) > 0;
    }

    private String getCompletionDate(int table) {
        String[] dates = getCompletionDates(table);

        if (dates.length > 0) {
            return dates[dates.length - 1];
        }

        return prefs.getString(keyCompletedDate(table), "brak daty");
    }

    private int getCompletionCount(int table) {
        return getCompletionDates(table).length;
    }

    private String[] getCompletionDates(int table) {
        String raw = getCompletionDatesRaw(table);

        if (raw.isEmpty()) {
            String oldDate = prefs.getString(keyCompletedDate(table), "");
            if (!oldDate.isEmpty() && prefs.getBoolean(keyCompleted(table), false)) {
                return new String[] { oldDate };
            }

            return new String[0];
        }

        return raw.split("\\|");
    }

    private String getCompletionDatesRaw(int table) {
        return prefs.getString(keyCompletedDates(table), "");
    }

    private String buildCompletionDatesText(int table) {
        String[] dates = getCompletionDates(table);

        if (dates.length == 0) {
            return "brak zapisanych dat";
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < dates.length; i++) {
            builder.append("🏆 ")
                    .append(i + 1)
                    .append(". ")
                    .append(dates[i]);

            if (i < dates.length - 1) {
                builder.append("\n");
            }
        }

        return builder.toString();
    }

    private void resetTableProgress(int table) {
        prefs.edit()
                .putInt(keyStreak(table, 1), 0)
                .putInt(keyStreak(table, 2), 0)
                .putInt(keyStreak(table, 3), 0)
                .putInt(keyStreak(table, 4), 0)
                .putInt(keyStreak(table, 5), 0)
                .putInt(keyStreak(table, 6), 0)
                .putInt(keyStreak(table, 7), 0)
                .putInt(keyStreak(table, 8), 0)
                .putInt(keyStreak(table, 9), 0)
                .putInt(keyStreak(table, 10), 0)
                .apply();
    }

    private int getStreak(int table, int multiplier) {
        return prefs.getInt(keyStreak(table, multiplier), 0);
    }

    private void saveStreak(int table, int multiplier, int streak) {
        prefs.edit()
                .putInt(keyStreak(table, multiplier), streak)
                .apply();
    }

    private int getStars() {
        return prefs.getInt(keyStars(), 0);
    }

    private void addStars(int amount) {
        if (amount <= 0) {
            return;
        }

        int newTotal = getStars() + amount;
        prefs.edit().putInt(keyStars(), newTotal).apply();
        updateRewardsHeader();
    }

    private int getDailyCompletedCount() {
        return prefs.getInt(keyDailyCompletedCount(), 0);
    }

    private void increaseDailyCompletedCount() {
        int newCount = getDailyCompletedCount() + 1;
        prefs.edit().putInt(keyDailyCompletedCount(), newCount).apply();
        updateRewardsHeader();
    }

    private String getToday() {
        return new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());
    }

    private String keyStreak(int table, int multiplier) {
        return "streak_" + table + "_" + multiplier;
    }

    private String keyCompleted(int table) {
        return "completed_" + table;
    }

    private String keyCompletedDate(int table) {
        return "completed_date_" + table;
    }

    private String keyCompletedDates(int table) {
        return "completed_dates_" + table;
    }

    private String keyTestLastScore() {
        return "test_last_score";
    }

    private String keyTestLastDate() {
        return "test_last_date";
    }

    private String keyTestBestScore() {
        return "test_best_score";
    }

    private String keyTestCompletedDate() {
        return "test_completed_date";
    }

    private String keyTestLastRawSeconds() {
        return "test_last_raw_seconds";
    }

    private String keyTestLastPenaltySeconds() {
        return "test_last_penalty_seconds";
    }

    private String keyTestLastFinalSeconds() {
        return "test_last_final_seconds";
    }

    private String keyTestLastWrongAnswers() {
        return "test_last_wrong_answers";
    }

    private String keyTestBestTimeSeconds() {
        return "test_best_time_seconds";
    }

    private String keyTestRanking() {
        return "test_ranking";
    }


    private String keyDailyLastScore() {
        return "daily_last_score";
    }

    private String keyDailyLastDate() {
        return "daily_last_date";
    }

    private String keyDailyBestScore() {
        return "daily_best_score";
    }

    private String keyDailyCompletedDate() {
        return "daily_completed_date";
    }

    private String keyDailyCompletedCount() {
        return "daily_completed_count";
    }

    private String keyStars() {
        return "total_stars";
    }

    private String getRandomHappyMessage() {
        return happyMessages[random.nextInt(happyMessages.length)];
    }

    private String getRandomTryAgainMessage() {
        return tryAgainMessages[random.nextInt(tryAgainMessages.length)];
    }


    private void setRobotFaceNeutral() {
        setRobotFaceImage(R.drawable.robot_neutral);
    }

    private void setRobotFaceHappy() {
        setRobotFaceImage(R.drawable.robot_happy);
    }

    private void setRobotFaceSad() {
        setRobotFaceImage(R.drawable.robot_sad);
    }

    private void setRobotFaceImage(int drawableRes) {
        if (robotFaceImageView == null) {
            return;
        }

        robotFaceImageView.setImageResource(drawableRes);
        robotFaceImageView.animate().cancel();
        robotFaceImageView.setScaleX(0.92f);
        robotFaceImageView.setScaleY(0.92f);
        robotFaceImageView.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(180)
                .start();
    }

    private void hideRetakeButton() {
        if (retakeButton != null) {
            retakeButton.setVisibility(View.GONE);
        }
    }

    private void showAnswerBar() {
        hideBannerAd();

        if (bottomAnswerBar != null) {
            bottomAnswerBar.setVisibility(View.VISIBLE);
            bottomAnswerBar.setTranslationY(0f);
        }
    }

    private void hideAnswerBar() {
        if (bottomAnswerBar != null) {
            bottomAnswerBar.setVisibility(View.GONE);
            bottomAnswerBar.setTranslationY(0f);
        }

        resetAnswerBarPosition();
    }

    private void hideBannerAd() {
        if (adViewContainer != null) {
            adViewContainer.setVisibility(View.GONE);
        }
    }

    private void showBannerAd() {
        /*
         * Nie pokazuj reklamy, jeśli aktywny jest pasek wpisywania odpowiedzi.
         */
        if (bottomAnswerBar != null && bottomAnswerBar.getVisibility() == View.VISIBLE) {
            return;
        }

        if (adViewContainer != null) {
            adViewContainer.setVisibility(View.VISIBLE);
        }
    }

    private void showKeyboardAndKeepInputVisible() {
        showAnswerBar();
        answerEditText.requestFocus();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            answerEditText.postDelayed(() ->
                    imm.showSoftInput(answerEditText, InputMethodManager.SHOW_IMPLICIT), 100
            );
            answerEditText.postDelayed(() ->
                    imm.showSoftInput(answerEditText, InputMethodManager.SHOW_IMPLICIT), 260
            );
        }

        /*
         * Najważniejsze:
         * - nie ma już dolnego paska najeżdżającego na zadanie,
         * - pole odpowiedzi jest bezpośrednio pod zadaniem,
         * - przewijamy tylko do obszaru: zadanie + pole + przycisk,
         *   nigdy do długiego komunikatu z gwiazdkami.
         */
        if (mainScrollView != null && bottomAnswerBar != null) {
            Runnable keepQuestionAndInputVisible = () -> {
                Rect rect = new Rect();
                bottomAnswerBar.getDrawingRect(rect);

                int distanceFromQuestionToAnswer =
                        Math.max(0, bottomAnswerBar.getTop() - questionTextView.getTop());

                rect.top -= distanceFromQuestionToAnswer + dp(12);
                rect.bottom += dp(16);

                mainScrollView.requestChildRectangleOnScreen(bottomAnswerBar, rect, true);
            };

            mainScrollView.postDelayed(keepQuestionAndInputVisible, 150);
            mainScrollView.postDelayed(keepQuestionAndInputVisible, 350);
            mainScrollView.postDelayed(keepQuestionAndInputVisible, 650);
        }
    }

    private void hideKeyboard() {

        View currentFocus = getCurrentFocus();

        if (currentFocus == null) {
            return;
        }

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm != null) {
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }

        currentFocus.clearFocus();
        resetAnswerBarPosition();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class TestResult {
        final String date;
        final int score;
        final int rawSeconds;
        final int penaltySeconds;
        final int finalSeconds;
        final int wrongAnswers;

        TestResult(String date, int score, int rawSeconds, int penaltySeconds, int finalSeconds, int wrongAnswers) {
            this.date = date;
            this.score = score;
            this.rawSeconds = rawSeconds;
            this.penaltySeconds = penaltySeconds;
            this.finalSeconds = finalSeconds;
            this.wrongAnswers = wrongAnswers;
        }
    }

    private static class Question {
        final int left;
        final int right;

        Question(int left, int right) {
            this.left = left;
            this.right = right;
        }
    }
}
