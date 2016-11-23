package com.example.shooting;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.viewport.FitViewport;

// アクター(actor)のアクション(action)を簡単に記述するためのstatic import
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.*;

// ゲームアセット
// [Space ship] http://opengameart.org/content/space-shooter-art
// [Beam sound] http://www.freesound.org/people/MusicLegends/sounds/344310/
//[BGM] http://www.freesound.org/people/orangefreesounds/sounds/326479/
public class Shooting extends ApplicationAdapter {
	private Stage stage;            // ゲームステージ
	private Image spaceship;        // スペースシップ (プレイヤー)
    private Sound beamSound;        // ビーム音
    private Music bgm;              // BGM
    private Integer beamCount = 0;  // ビーム発射数 (発射数制限を設けるため)

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
				if (beamCount < 3) {    // ビーム発射数が3発以下なら新たにビームを発射する
					beamCount++;        // ビーム発射数を1つ増やす
					Image beam = new Image(new Texture(Gdx.files.internal("beam.png")));    // ビーム用のアクター(actor)を用意する
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

		spaceship = new Image(new Texture(Gdx.files.internal("spaceship01.png")));   // スペースシップ(プレイヤー)用アクター(actor)を用意する
        // スペースシップを画面下端中央に配置する
        spaceship.setPosition(stage.getWidth() * 0.5f - spaceship.getWidth() * 0.5f, 0);
		spaceship.setZIndex(10);    // スペースシップが最前面に配置されるようにする
		stage.addActor(spaceship);  // スペースシップをステージに追加する

		beamSound = Gdx.audio.newSound(Gdx.files.internal("beam.wav"));     // ビーム発射音用サウンドを読み込む
        bgm = Gdx.audio.newMusic(Gdx.files.internal("bgm.mp3"));            // BGM用音楽を読み込む
        bgm.setLooping(true);   // BGM再生をループ設定にする
        bgm.play();             // BGMを再生する
    }

	@Override
	public void render () {
        // 画面をミッドナイトブルー(red = 44, green = 62, blue = 80)に設定する
        // 色参照: https://flatuicolors.com/
		Gdx.gl.glClearColor(44 / 255.f, 62 / 255.f, 80 / 255.f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);   // 画面をクリアする
		stage.act(Gdx.graphics.getDeltaTime());     // ステージの状態を前回render呼び出しからの経過時間(delta time)分だけ更新する
		stage.draw();                               // ステージを最新の状態に描画する

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

	@Override
	public void dispose () {
		stage.dispose();        // ステージを破棄する
        bgm.dispose();          // ビーム発射音を破棄する
        beamSound.dispose();    // BGMを破棄する
	}
}
