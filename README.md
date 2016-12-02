# シューティングゲーム
LibGDXを使って、簡単なシューティングゲームを作成します。<br/>
<img height="320" alt="ゲーム画面" src="https://github.com/kojima/simple-libgdx-shooting/blob/master/screenshots/sh01.png"/><br/>
* 画面をタップすると、<a href="http://opengameart.org/content/space-shooter-art">スペースシップ(プレイヤー)</a>からビームが発射されます。
* スペースシップは、端末を傾けて操作します。
* 一度に発射できるビームを3発に制限します。
* ビーム発射の際に、<a href="http://www.freesound.org/people/MusicLegends/sounds/344310/">発射音</a>が鳴るようにします。
* <a href="http://www.freesound.org/people/orangefreesounds/sounds/326479/">BGM</a>もループ再生します。
* LibGDXのActorやAction等、基本的な要素を使用して実現しています。

++++++++++++++++++++++++++++++
最初は、<a href="https://github.com/kojima/simple-libgdx-shooting/tree/step01">"Step 1"</a>から作り始めます
++++++++++++++++++++++++++++++

# AndroidManifest
`android:screenOrientation`を`portrait`に設定する
``` xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.shooting"
    android:versionCode="1"
    android:versionName="1.0" >

    <uses-sdk android:minSdkVersion="8" android:targetSdkVersion="25" />

    <application
        android:allowBackup="true"
        android:icon="@drawable/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/GdxTheme" >
        <activity
            android:name="com.example.shooting.AndroidLauncher"
            android:label="@string/app_name"
            android:screenOrientation="portrait"
            android:configChanges="keyboard|keyboardHidden|orientation|screenSize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

# メインコード(`Shooting.java`)
``` java
package com.example.shooting;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
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

    private Stage stage;                // ゲームステージ
    private GameSprite spaceship;       // スペースシップ (プレイヤー)
    private Sound beamSound;            // ビーム音
    private Sound explosionSound;       // 爆発音
    private Sound enemySpawnSound;      // 敵発生音
    private Sound enemyBeamSound;       // 敵ビーム音
    private Sound enemyExplosionSound;  // 敵爆発音
    private Sound gameLoseSound;        // ゲームオーバー音
    private Music bgm;                  // BGM
    private Integer beamCount = 0;      // ビーム発射数 (発射数制限を設けるため)
    private long lastEnemySpawnedTime;  // 最後に敵を発生させた時間
    private boolean gameOver = false;   // ゲームオーバーしているかどうか (trueの場合ゲームオーバー)

    @Override
    public void create () {
        stage = new Stage(new FitViewport(1080, 1776));     // ゲーム用のステージを1080x1776のサイズで作成
        Gdx.input.setInputProcessor(stage);                 // ステージでインプット(タッチ入力など)を処理する
        // ステージでタッチ入力を処理するためのリスナー(listener)を追加する
        stage.addListener(new InputListener() {
            public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }

            // タッチアップ(タッチして指を離したタイミング)でビームを発射する
            public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
                if (gameOver) return;

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
        });

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

        spaceship = new GameSprite(new Texture(Gdx.files.internal("spaceship01.png")));   // スペースシップ(プレイヤー)用アクター(actor)を用意する
        spaceship.name = "spaceship";
        // スペースシップを画面下端中央に配置する
        spaceship.setPosition(stage.getWidth() * 0.5f - spaceship.getWidth() * 0.5f, 0);
        spaceship.setZIndex(10);    // スペースシップが最前面に配置されるようにする
        stage.addActor(spaceship);  // スペースシップをステージに追加する

        beamSound = Gdx.audio.newSound(Gdx.files.internal("beam.wav"));                         // ビーム発射音用サウンドを読み込む
        explosionSound = Gdx.audio.newSound(Gdx.files.internal("explosion.wav"));               // 爆発用サウンドを読み込む
        enemySpawnSound = Gdx.audio.newSound(Gdx.files.internal("enemy_spawn.wav"));            // 敵発生音用サウンドを読み込む
        enemyBeamSound = Gdx.audio.newSound(Gdx.files.internal("enemy_beam.wav"));              // 敵ビーム用サウンドを読み込む
        enemyExplosionSound = Gdx.audio.newSound(Gdx.files.internal("enemy_explosion.wav"));    // 敵爆発用サウンドを読み込む
        gameLoseSound = Gdx.audio.newSound(Gdx.files.internal("lose.wav"));                     // ゲームオーバー用サウンドを読み込む
        bgm = Gdx.audio.newMusic(Gdx.files.internal("bgm.mp3"));                                // BGM用音楽を読み込む
        bgm.setLooping(true);   // BGM再生をループ設定にする
        bgm.play();             // BGMを再生する

        lastEnemySpawnedTime = TimeUtils.nanoTime();
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

        if (gameOver) return;

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

        // ランダムな間隔(3秒〜6秒)で敵を発生させる
        if (TimeUtils.nanoTime() - lastEnemySpawnedTime > (1000000000 * (long)MathUtils.random(3, 6))) spawnEnemy();

        // ゲームキャラクター同士に衝突がないかチェックする
        checkCollisions();
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
                                explodePlayer(spriteA);
                            } else {
                                explodePlayer(spriteB);
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
        gameOver = true;
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
        stage.addActor(explosion);
        enemy.remove();
        enemyExplosionSound.play();
    }

    // ゲームオーバーの演出を行う
    private void gameOver() {
        bgm.stop();
        gameLoseSound.play();
    }

    @Override
    public void dispose () {
        stage.dispose();                // ステージを破棄する
        beamSound.dispose();            // ビーム発射音を破棄する
        enemySpawnSound.dispose();      // 敵発生音を破棄する
        enemyBeamSound.dispose();       // 敵ビーム音を破棄する
        enemyExplosionSound.dispose();  // 敵爆発音を破棄する
        gameLoseSound.dispose();        // ゲームオーバー音を破棄する
        bgm.dispose();                  // BGMを破棄する
    }
}
```

# 使用ゲームアセット
## 画像
* <a href="http://opengameart.org/content/space-shooter-art">スペースシップ/敵/ビーム</a>

## サウンド
* <a href="http://www.freesound.org/people/MusicLegends/sounds/344310/">ビーム音</a>
* <a href="http://www.freesound.org/people/alpharo/sounds/186696/">敵発生音</a>
* <a href="http://www.freesound.org/people/Heshl/sounds/269170/">敵ビーム音</a>
* <a href="http://www.freesound.org/people/LittleRobotSoundFactory/sounds/270310/">敵爆発音</a>
* <a href="http://www.freesound.org/people/LittleRobotSoundFactory/sounds/270329/">ゲームオーバー音</a>
* <a href="http://www.freesound.org/people/orangefreesounds/sounds/326479/">BGM</a>
