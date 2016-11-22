package com.example.shooting;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.forever;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.removeActor;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.run;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

public class Shooting extends ApplicationAdapter {
	private Stage stage;
	private Image spaceship;
	private Sound beamSound;
	private Integer beamCount = 0;

	@Override
	public void create () {
		stage = new Stage(new ScreenViewport());
		Gdx.input.setInputProcessor(stage);
		stage.addListener(new InputListener() {
			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
				return true;
			}

			public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
				if (beamCount < 3) {
					beamCount++;
					Image beam = new Image(new Texture(Gdx.files.internal("beam.png")));
					beam.setPosition(spaceship.getX() + spaceship.getWidth() * .5f - beam.getWidth() * .5f, spaceship.getY() + spaceship.getHeight() * .5f);
					beam.setZIndex(5);
					beamSound.play();
					stage.addActor(beam);
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

		Image starBack = new Image(new Texture(Gdx.files.internal("star_back.png")));
		starBack.setZIndex(1);
		starBack.addAction(forever(
				sequence(
						moveTo(0, -stage.getHeight(), 7),
						moveTo(0, 0)
				)
		));
		stage.addActor(starBack);

		Image starFront = new Image(new Texture(Gdx.files.internal("star_front.png")));
		starFront.setZIndex(3);
		starFront.addAction(forever(
				sequence(
						moveTo(0, -stage.getHeight(), 5),
						moveTo(0, 0)
				)
		));
		stage.addActor(starFront);

		spaceship = new Image(new Texture(Gdx.files.internal("spaceship01.png")));
		spaceship.setZIndex(10);
		stage.addActor(spaceship);

		beamSound = Gdx.audio.newSound(Gdx.files.internal("beam.wav"));
	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(44 / 255.f, 62 / 255.f, 80 / 255.f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		stage.act(Gdx.graphics.getDeltaTime());
		stage.draw();

		if (Math.abs(Gdx.input.getAccelerometerX()) > 0.2) {
			spaceship.setX(spaceship.getX() - 200 * Gdx.input.getAccelerometerX() * Gdx.graphics.getDeltaTime());
		}
		if (Math.abs(Gdx.input.getAccelerometerY()) > 0.2) {
			spaceship.setY(spaceship.getY() - 200 * Gdx.input.getAccelerometerY() * Gdx.graphics.getDeltaTime());
		}

		if (spaceship.getX() < 0) {
			spaceship.setX(0);
		} else if (spaceship.getX() > stage.getWidth() - spaceship.getWidth()) {
			spaceship.setX(stage.getWidth() - spaceship.getWidth());
		}
		if (spaceship.getY() < 0) {
			spaceship.setY(0);
		} else if (spaceship.getY() > stage.getHeight() - spaceship.getHeight()) {
			spaceship.setY(stage.getHeight() - spaceship.getHeight());
		}
	}

	@Override
	public void dispose () {
		stage.dispose();
	}
}
