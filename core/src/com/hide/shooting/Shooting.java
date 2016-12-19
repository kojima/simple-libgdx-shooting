package com.hide.shooting;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.FitViewport;

// アクター(actor)のアクション(action)を簡単に記述するためのstatic import
import java.util.Arrays;
import java.util.List;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.*;

// ゲームアセット
// [Space ship] http://opengameart.org/content/space-shooter-art
// [Beam sound] http://www.freesound.org/people/MusicLegends/sounds/344310/
// [Enemy spawn sound] http://www.freesound.org/people/alpharo/sounds/186696/
// [Enemy beam sound] http://www.freesound.org/people/Heshl/sounds/269170/
// [Enemy explosion sound] http://www.freesound.org/people/LittleRobotSoundFactory/sounds/270310/
// [Game lose sound] http://www.freesound.org/people/LittleRobotSoundFactory/sounds/270329/
// [Game win sound] http://www.freesound.org/people/LittleRobotSoundFactory/sounds/270333/
// [Button tap sound] http://www.freesound.org/people/LittleRobotSoundFactory/sounds/270324/
// [BGM] http://www.freesound.org/people/orangefreesounds/sounds/326479/
public class Shooting extends ApplicationAdapter {

    // ゲーム中のキャラクター用のクラスを定義する
    private static final class GameSprite extends Image {

        String name = "";
        Rectangle bounds = new Rectangle();

        private GameSprite(Texture texture) {
            super(texture);
            bounds.setWidth(getWidth());
            bounds.setHeight(getHeight());
        }

        @Override
        public void draw (Batch batch, float parentAlpha) {
            super.draw(batch, parentAlpha);
            bounds.setPosition(getX(), getY());
        }

        private boolean overlaps(GameSprite sprite) {
            return bounds.overlaps(sprite.bounds);
        }
    }

    // ゲーム中に表示するテキスト用のクラスを定義する
    private static final class GameText extends Actor {

        String text = "";
        BitmapFont font = new BitmapFont(Gdx.files.internal("88zen.fnt"));

        @Override
        public void draw(Batch batch, float parentAlpha) {
            super.draw(batch, parentAlpha);
            font.draw(batch, text, getX(), getY());
        }
    }

    // ゲームの残り距離を表示するためのクラスを定義する
    private static final class DistanceMeter extends Actor {

        int currentDistance = 0;
        private ShapeRenderer renderer = new ShapeRenderer();

        public DistanceMeter(float x, float y, float width, float height) {
            super();
            setPosition(x, y);
            setWidth(width);
            setHeight(height);
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            super.draw(batch, parentAlpha);
            batch.end();
            renderer.setProjectionMatrix(batch.getProjectionMatrix());
            renderer.setTransformMatrix(batch.getTransformMatrix());
            renderer.translate(getX(), getY(), 0);
            renderer.begin(ShapeRenderer.ShapeType.Filled);
            renderer.setColor(1, 1, 1, 1);
            renderer.rect(0, 0, getWidth(), getHeight());
            renderer.setColor(26 / 255.f, 188 / 255.f, 156 / 255.f, 1);
            renderer.rect(0, 0, getWidth(), getHeight() * currentDistance / 100.f);
            renderer.end();
            batch.begin();
        }
    }

    // ゲームの状態を管理するための列挙型を定義する
    private enum GameStatus {
        PLAYING,
        GAME_WIN,
        GAME_OVER,
        WAIT_TO_RESTART_FROM_WIN,
        WAIT_TO_RESTART_FROM_LOSE,
    }

    private Stage stage;                // ゲームステージ
    private GameSprite spaceship;       // スペースシップ (プレイヤー)
    private GameText scoreText;         // ゲームスコア表示
    private DistanceMeter meter;        // 残り距離表示
    private Image youWin;               // ゲームクリア
    private Image gameOver;             // ゲームオーバー
    private Sound beamSound;            // ビーム音
    private Sound explosionSound;       // 爆発音
    private Sound enemySpawnSound;      // 敵発生音
    private Sound enemyBeamSound;       // 敵ビーム音
    private Sound enemyExplosionSound;  // 敵爆発音
    private Sound gameLoseSound;        // ゲームオーバー音
    private Sound gameWinSound;         // ゲームウィン音
    private Sound tapSound;             // タップ音
    private Music bgm;                  // BGM
    private Integer beamCount = 0;      // ビーム発射数 (発射数制限を設けるため)
    private long lastEnemySpawnedTime;  // 最後に敵を発生させた時間
    private int score = 0;              // 現在のゲームスコア
    private GameStatus status = GameStatus.PLAYING; // ゲームステータス
    private InputListener inputListener;            // ステージ用イベントリスナ
    private long gameStartTime;         // ゲーム開始時刻

    @Override
    public void create () {
        stage = new Stage(new FitViewport(1080, 1776));     // ゲーム用のステージを1080x1776のサイズで作成
        Gdx.input.setInputProcessor(stage);                 // ステージでインプット(タッチ入力など)を処理する
        // ステージ用のイベントリスナを定義する
        inputListener = new InputListener() {
            public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }

            // タッチアップ(タッチして指を離したタイミング)でビームを発射する
            public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
                if (status == GameStatus.GAME_OVER ||
                        status == GameStatus.WAIT_TO_RESTART_FROM_WIN ||
                        status == GameStatus.WAIT_TO_RESTART_FROM_LOSE) return;

                if (beamCount < 3) {    // ビーム発射数が3発以下なら新たにビームを発射する
                    beamCount++;        // ビーム発射数を1つ増やす
                    GameSprite beam = new GameSprite(new Texture(Gdx.files.internal("beam.png")));    // ビーム用のアクター(actor)を用意する
                    beam.name = "beam";
                    // ビームがスペースシップの先端中央から発射されるように位置を設定する
                    beam.setPosition(spaceship.getX() + spaceship.getWidth() * .5f - beam.getWidth() * .5f, spaceship.getY() + spaceship.getHeight() * .5f);
                    beam.setZIndex(5);      // ビームがスペースシップより下に配置されるようにする
                    beamSound.play();       // ビーム発射音を鳴らす
                    stage.addActor(beam);   // ビーム用のアクターをゲームステージに追加する
                    // ビーム用のアクターに以下のアクションを追加する:
                    // 1. ステージの高さの分だけ0.5秒で前に進む
                    // 2. ビーム発射数を1つ減らす
                    // 3. ビーム用アクターをステージから削除する
                    beam.addAction(sequence(
                            moveTo(beam.getX(), beam.getY() + stage.getHeight(), .5f),
                            run(new Runnable() {
                                @Override
                                public void run() {
                                    beamCount--;
                                }
                            }),
                            removeActor()
                    ));
                }
            }
        };
        // ステージでタッチ入力を処理するためのリスナー(listener)を追加する
        stage.addListener(inputListener);

        Image starBack = new Image(new Texture(Gdx.files.internal("star_back.png")));   // 宇宙の星(後背景)用アクター(actor)を用意する
        starBack.setZIndex(1);          // 宇宙の星(後背景)がスペースシップやビームより下に配置されるようにする
        // 宇宙の星(後背景)に以下のアクションを追加する:
        // 1. ステージの高さの分だけ7秒で後に進む
        // 2. 元の位置(x = 0, y = 0)に戻る
        // ※ 1 → 2をずっと繰り返す
        starBack.addAction(forever(
            sequence(
                moveTo(0, -stage.getHeight(), 7),
                moveTo(0, 0)
            )
        ));
        stage.addActor(starBack);   // 宇宙の星(後背景)をステージに追加する

        Image starFront = new Image(new Texture(Gdx.files.internal("star_front.png")));   // 宇宙の星(前背景)用アクター(actor)を用意する
        starFront.setZIndex(3);          // 宇宙の星(前背景)がスペースシップやビームより下に配置されるようにする (宇宙の星(後背景)よりは前に配置されるようにする)
        // 宇宙の星(前背景)に以下のアクションを追加する:
        // 1. ステージの高さの分だけ5秒で後に進む
        // 2. 元の位置(x = 0, y = 0)に戻る
        // ※ 1 → 2をずっと繰り返す
        starFront.addAction(forever(
            sequence(
                moveTo(0, -stage.getHeight(), 5),
                moveTo(0, 0)
            )
        ));
        stage.addActor(starFront);   // 宇宙の星(前背景)をステージに追加する

        // 残り距離を画面右端に表示する
        meter = new DistanceMeter(stage.getWidth() - 20, 0, 20, stage.getHeight());
        meter.currentDistance = 0;
        stage.addActor(meter);

        // ゲームスコアを画面左上に表示する
        scoreText = new GameText();
        scoreText.text = "スコア: " + score;
        scoreText.setPosition(32, stage.getHeight() - 40);
        scoreText.setZIndex(100);   // ゲームスコアが最前面に配置されるようにする
        stage.addActor(scoreText);

        spaceship = new GameSprite(new Texture(Gdx.files.internal("spaceship01.png")));   // スペースシップ(プレイヤー)用アクター(actor)を用意する
        spaceship.name = "spaceship";
        // スペースシップを画面下端中央に配置する
        spaceship.setPosition(stage.getWidth() * 0.5f - spaceship.getWidth() * 0.5f, 0);
        spaceship.setZIndex(10);    // スペースシップが前面に配置されるようにする
        stage.addActor(spaceship);  // スペースシップをステージに追加する

        // ゲームクリアメッセージ
        youWin = new Image(new Texture(Gdx.files.internal("you_win.png")));
        youWin.setPosition(0, stage.getHeight() * .5f - youWin.getHeight() * .5f);

        // ゲームオーバーメッセージ
        gameOver = new Image(new Texture(Gdx.files.internal("game_over.png")));
        gameOver.setPosition(0, stage.getHeight() * .5f - gameOver.getHeight() * .5f);

        beamSound = Gdx.audio.newSound(Gdx.files.internal("beam.wav"));                         // ビーム発射音用サウンドを読み込む
        explosionSound = Gdx.audio.newSound(Gdx.files.internal("explosion.wav"));               // 爆発用サウンドを読み込む
        enemySpawnSound = Gdx.audio.newSound(Gdx.files.internal("enemy_spawn.wav"));            // 敵発生音用サウンドを読み込む
        enemyBeamSound = Gdx.audio.newSound(Gdx.files.internal("enemy_beam.wav"));              // 敵ビーム用サウンドを読み込む
        enemyExplosionSound = Gdx.audio.newSound(Gdx.files.internal("enemy_explosion.wav"));    // 敵爆発用サウンドを読み込む
        gameLoseSound = Gdx.audio.newSound(Gdx.files.internal("lose.wav"));                     // ゲームオーバー用サウンドを読み込む
        gameWinSound = Gdx.audio.newSound(Gdx.files.internal("win.wav"));                       // ゲームウィン用サウンドを読み込む
        tapSound = Gdx.audio.newSound(Gdx.files.internal("tap.wav"));                           // ボタンタップ用サウンドを読み込む
        bgm = Gdx.audio.newMusic(Gdx.files.internal("bgm.mp3"));                                // BGM用音楽を読み込む
        bgm.setLooping(true);   // BGM再生をループ設定にする
        bgm.play();             // BGMを再生する

        lastEnemySpawnedTime = TimeUtils.nanoTime();    // 最終敵生成時刻を初期化する
        gameStartTime = TimeUtils.nanoTime();           // ゲーム開始時刻を初期化する
    }

    private void spawnEnemy() {
        final GameSprite enemyShip = new GameSprite(new Texture(Gdx.files.internal("enemy_ship.png")));
        enemyShip.name = "enemy";
        enemyShip.setX(MathUtils.random(0, stage.getWidth() - enemyShip.getWidth()));
        enemyShip.setY(stage.getHeight());
        // 画面を3秒〜6秒の時間で縦に移動するようにアクションを設定する
        // それと同時に、横方向に不規則に動くようにもアクションを設定する
        enemyShip.addAction(parallel(
            // 縦に移動するためのアクション
            sequence(
                moveBy(0, -(stage.getHeight() + enemyShip.getHeight()), MathUtils.random(3, 6)),
                removeActor()
            ),
            // 横方向に不規則に動くためのアクション
            forever(
                sequence(
                    delay(MathUtils.random(50, 100) / 100.f),
                    // 毎回違う量だけ横に動かすためには、runアクション内でmoveByアクションを設定する必要がある
                    run(new Runnable() {
                        @Override
                        public void run() {
                        enemyShip.addAction(moveBy(MathUtils.random(-200, 200), 0, .5f));
                        }
                    })
                )

            )
        ));
        // 不規則な間隔(0.5秒〜3秒)でビームを撃ち続ける
        enemyShip.addAction(forever(
            sequence(
                delay(MathUtils.random(50, 300) / 100.f),
                run(new Runnable() {
                    @Override
                    public void run() {
                        spawnEnemyBeam(enemyShip);
                    }
                })
            )
        ));
        enemySpawnSound.play();
        stage.addActor(enemyShip);
        lastEnemySpawnedTime = TimeUtils.nanoTime();
    }

    private void spawnEnemyBeam(Actor enemy) {
        GameSprite beam = new GameSprite(new Texture(Gdx.files.internal("enemy_beam.png")));
        beam.name = "enemy_beam";
        beam.setPosition(enemy.getX() + enemy.getWidth() * .5f - beam.getWidth() * 0.5f, enemy.getY());
        beam.addAction(sequence(
            moveBy(0, -stage.getHeight(), 1.f),
            removeActor()
        ));
        stage.addActor(beam);
        enemyBeamSound.play();
    }

    @Override
    public void render () {
        // 画面をミッドナイトブルー(red = 44, green = 62, blue = 80)に設定する
        // 色参照: https://flatuicolors.com/
        Gdx.gl.glClearColor(44 / 255.f, 62 / 255.f, 80 / 255.f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);   // 画面をクリアする
        stage.act(Gdx.graphics.getDeltaTime());     // ステージの状態を前回render呼び出しからの経過時間(delta time)分だけ更新する
        stage.draw();                               // ステージを最新の状態に描画する

        if (status == GameStatus.GAME_OVER) {
            return;
        } else if (status == GameStatus.GAME_WIN) {
            controlPlayer();
        } else if (status == GameStatus.WAIT_TO_RESTART_FROM_WIN) {
            controlPlayer();
        } else if (status == GameStatus.WAIT_TO_RESTART_FROM_LOSE) {
            if (Gdx.input.isTouched()) restart();
        } else if (status == GameStatus.PLAYING) {
            controlPlayer();
            // ランダムな間隔(3秒〜6秒)で敵を発生させる
            if (TimeUtils.nanoTime() - lastEnemySpawnedTime > (1000000000 * (long)MathUtils.random(3, 6))) spawnEnemy();
            // ゲーム開始時刻からの経過時間から、進行距離を計算する
            meter.currentDistance = (int)((TimeUtils.nanoTime() - gameStartTime) / 1000000000.f);
            // 進行距離が100を超えたらゲームクリア
            if (meter.currentDistance > 100) gameWin();
            // ゲームキャラクター同士に衝突がないかチェックする
            checkCollisions();
        }
    }

    // プレイヤーを操縦する
    private void controlPlayer() {
        // 端末が横方向に傾いたら、傾き量に応じてスペースシップを横方向に移動させる
        if (Math.abs(Gdx.input.getAccelerometerX()) > 0.2) {
            spaceship.setX(spaceship.getX() - 200 * Gdx.input.getAccelerometerX() * Gdx.graphics.getDeltaTime());
        }
        // 端末が縦方向に傾いたら、傾き量に応じてスペースシップを縦方向に移動させる
        if (Math.abs(Gdx.input.getAccelerometerY()) > 0.2) {
            spaceship.setY(spaceship.getY() - 200 * Gdx.input.getAccelerometerY() * Gdx.graphics.getDeltaTime());
        }

        if (spaceship.getX() < 0) { // スペースシップが画面左端よりも左に移動してしまったら、画面左端に戻す
            spaceship.setX(0);
        } else if (spaceship.getX() > stage.getWidth() - spaceship.getWidth()) {  // スペースシップが画面右端よりも右に移動してしまったら、画面右端に戻す
            spaceship.setX(stage.getWidth() - spaceship.getWidth());
        }
        if (spaceship.getY() < 0) { // スペースシップが画面下端よりも下に移動してしまったら、画面下端に戻す
            spaceship.setY(0);
        } else if (spaceship.getY() > stage.getHeight() - spaceship.getHeight()) {  // スペースシップが画面上端よりも上に移動してしまったら、画面上端に戻す
            spaceship.setY(stage.getHeight() - spaceship.getHeight());
        }
    }

    // ゲーム中のキャラクターの衝突をチェックする
    private void checkCollisions() {
        Array<Actor> actors = stage.getActors();
        for (int i = 0; i < actors.size; i++) {
            Actor actorA = actors.get(i);
            for (int j = i + 1; j < actors.size; j++) {
                Actor actorB = actors.get(j);
                // actorAもactorBもどちらもゲームキャラクター(GameSprite)の場合
                if (actorA instanceof GameSprite && actorB instanceof GameSprite) {
                    GameSprite spriteA = (GameSprite)actorA;
                    GameSprite spriteB = (GameSprite)actorB;
                    List<String> names = Arrays.asList(spriteA.name, spriteB.name);
                    if (spriteA.overlaps(spriteB)) {
                        // プレイヤーが敵または敵のビームに触れた場合
                        if (names.contains("spaceship") && (names.contains("enemy") || names.contains("enemy_beam"))) {
                            // spriteAとspriteBのどちらがプレイヤーか調べる
                            if (spriteA.name.equals("spaceship")) {
                                if (spriteB.getImageWidth() > 0) explodePlayer(spriteA);
                            } else {
                                if (spriteA.getImageWidth() > 0) explodePlayer(spriteB);
                            }
                        } else if (names.contains("enemy") && names.contains("beam")) { // 敵がビームに触れた場合
                            // spriteAとspriteBのどちらが敵か調べる
                            if (spriteA.name.equals("enemy")) {
                                explodeEnemy(spriteA);
                            } else {
                                explodeEnemy(spriteB);
                            }
                        }
                    }
                }
            }
        }
    }

    // プレイヤーを爆破させる
    private void explodePlayer(GameSprite player) {
        Image explosion = new Image(new Texture(Gdx.files.internal("explosion.png")));
        explosion.setPosition(player.getX(), player.getY());
        explosion.setOrigin(explosion.getWidth() * .5f, explosion.getHeight() * .5f);
        Color color = explosion.getColor();
        explosion.setScale(0, 0);
        explosion.setColor(color.r, color.g, color.b, 0.f);
        explosion.addAction(parallel(
                sequence(
                        fadeIn(.2f),
                        delay(.5f),
                        fadeOut(1.5f),
                        removeActor()
                ),
                scaleTo(2.f, 2.f, .2f)
        ));
        status = GameStatus.GAME_OVER;  // ステータスをゲームオーバーにする
        // 爆発が終わった後(2秒後)にゲームオーバーの演出をする
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                gameOver();
            }
        }, 2.f);
        stage.addActor(explosion);
        player.remove();
        explosionSound.play();
    }

    // 敵を爆破させる
    private void explodeEnemy(GameSprite enemy) {
        Image explosion = new Image(new Texture(Gdx.files.internal("enemy_explosion.png")));
        explosion.setPosition(enemy.getX(), enemy.getY());
        explosion.setOrigin(explosion.getWidth() * .5f, explosion.getHeight() * .5f);
        Color color = explosion.getColor();
        explosion.setScale(0, 0);
        explosion.setColor(color.r, color.g, color.b, 0.f);
        explosion.addAction(parallel(
                sequence(
                    fadeIn(.2f),
                    delay(.5f),
                    fadeOut(1.5f),
                    removeActor()
                ),
                scaleTo(2.f, 2.f, .2f)
        ));
        score += 10;    // スコアを10点加算する
        scoreText.text = "スコア: " + score;
        stage.addActor(explosion);
        enemy.remove();
        enemyExplosionSound.play();
    }

    // ゲームウィンの演出を行う
    private void gameWin() {
        status = GameStatus.GAME_WIN;
        gameWinSound.play();
        Array<Actor> actors = stage.getActors();
        for (int i = 0; i < actors.size; i++) {
            Actor actor = actors.get(i);
            if (actor instanceof GameSprite) {
                GameSprite sprite = (GameSprite)actor;
                if (sprite.name.equals("enemy")) {
                    explodeEnemy(sprite);
                } else if (sprite.name.equals("enemy_beam")) {
                    sprite.remove();
                }
            }
        }
        // ゲームウィン表示を点滅させる
        youWin.addAction(
            repeat(3, sequence(fadeOut(.2f), fadeIn(.2f), delay(.2f)))
        );
        stage.addActor(youWin);
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                // ゲームスコア表示を画面中央に移動させる
                GlyphLayout layout = new GlyphLayout();
                layout.setText(scoreText.font, scoreText.text);
                scoreText.setPosition(stage.getWidth() * .5f - layout.width * .5f, stage.getHeight() * .5f - 128.f);
                // リスタートボタンをセットアップする
                final Image restartButton = new Image(new Texture(Gdx.files.internal("restart_button.png")));
                restartButton.addListener(new InputListener() {
                    public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                        event.stop();
                        return true;
                    }

                    public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
                        tapSound.play();
                        restartButton.remove();
                        restart();
                    }
                });
                restartButton.setPosition(stage.getWidth() * .5f - restartButton.getWidth() * .5f, stage.getHeight() * .5f - 400.f);
                stage.addActor(restartButton);
                status = GameStatus.WAIT_TO_RESTART_FROM_WIN;    // ステータスをリスタート待ち(wait to restart)にする
            }
        }, 2.5f);
    }

    // ゲームオーバーの演出を行う
    private void gameOver() {
        bgm.stop(); // BGMを停止する
        stage.removeListener(inputListener);    // ステージから一旦イベントリスナを削除する
        gameLoseSound.play();   // ゲームオーバー音を鳴らす
        // ゲームオーバー音がなり終わった後にゲームオーバー画面を表示する
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                // スコアを非表示にする
                scoreText.remove();
                stage.addActor(gameOver);
                status = GameStatus.WAIT_TO_RESTART_FROM_LOSE;    // ステータスをリスタート待ち(wait to restart)にする
            }
        }, 4.5f);
    }

    // ゲームリスタート時のセットアップを行う
    private void restart() {
        if (status == GameStatus.WAIT_TO_RESTART_FROM_LOSE) {
            // スペースシップを配置し直し、ステージに再度追加する
            spaceship.setPosition(stage.getWidth() * 0.5f - spaceship.getWidth() * 0.5f, 0);
            stage.addActor(spaceship);
            // ステージにイベントリスナを再度追加する
            stage.addListener(inputListener);
            // ゲームオーバー画像を削除する
            gameOver.remove();
            // スコアを再度表示する
            stage.addActor(scoreText);
        } else if (status == GameStatus.WAIT_TO_RESTART_FROM_WIN) {
            // ゲームクリア画像を削除する
            youWin.remove();
            // スコア表示を画面左上に戻す
            scoreText.setPosition(32, stage.getHeight() - 40);
        }
        // BGMを最初から再生する
        bgm.setPosition(0);
        bgm.play();
        status = GameStatus.PLAYING;
        // スコアを0にリセットする
        score = 0;
        scoreText.text = "スコア: " + score;
        // ゲーム開始時刻を現在にセットする
        gameStartTime = TimeUtils.nanoTime();
    }

    @Override
    public void dispose () {
        stage.dispose();                // ステージを破棄する
        beamSound.dispose();            // ビーム発射音を破棄する
        enemySpawnSound.dispose();      // 敵発生音を破棄する
        enemyBeamSound.dispose();       // 敵ビーム音を破棄する
        enemyExplosionSound.dispose();  // 敵爆発音を破棄する
        gameLoseSound.dispose();        // ゲームオーバー音を破棄する
        gameWinSound.dispose();         // ゲームウィン音を破棄する
        tapSound.dispose();             // タップ音を破棄する
        bgm.dispose();                  // BGMを破棄する
    }
}
