/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2020 Raymond Buckley
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package com.ray3k.jbumpexample.tank;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.SnapshotArray;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.dongbat.jbump.Item;
import com.dongbat.jbump.Rect;
import com.dongbat.jbump.World;
import space.earlygrey.shapedrawer.ShapeDrawer;

/**
 * Initiates the main logic of the game, runs the main game loop, and renders the entities.
 */
public class Core extends ApplicationAdapter {
	public static SpriteBatch spriteBatch;
	public static TextureAtlas textureAtlas;
	public static ShapeDrawer shapeDrawer;
	public static ExtendViewport viewport;
	public static OrthographicCamera camera;
	public static SnapshotArray<Entity> entities;
	public static World<Entity> world;
	public static final float ENEMY_DELAY = .5f;
	public float enemyTimer;
	public static Player player;
	public static final Vector2 vector2 = new Vector2();

	@Override
	public void create() {
		spriteBatch = new SpriteBatch();
		textureAtlas = new TextureAtlas("textures.atlas");
		camera = new OrthographicCamera();
		viewport = new ExtendViewport(800, 800, camera);
		entities = new SnapshotArray<>();
		world = new World<>();
		shapeDrawer = new ShapeDrawer(spriteBatch, textureAtlas.findRegion("white"));
		
		//load map and spawn entities
		loadMap();
	}
	
	private void loadMap() {
		for (Entity entity : entities) {
			if (entity.item != null) world.remove(entity.item);
		}
		entities.clear();
		
		player = new Player();
	}

	@Override
	public void render() {
		if (Gdx.input.isKeyJustPressed(Keys.F5)) {
			loadMap();
		}
		
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		viewport.apply();
		spriteBatch.setProjectionMatrix(camera.combined);
		spriteBatch.begin();
		
		float delta = Gdx.graphics.getDeltaTime();
		
		//create enemies
		enemyTimer -= delta;
		if (enemyTimer < 0) {
			enemyTimer = ENEMY_DELAY;
			
			Enemy enemy = new Enemy();
			vector2.set(camera.viewportWidth / 2, camera.viewportHeight / 2);
			vector2.rotate(MathUtils.random(360f));
			vector2.add(camera.position.x, camera.position.y);
			enemy.x = vector2.x;
			enemy.y = vector2.y;
		}
		
		//call logic on all entities
		Object[] ents = entities.begin();
		for (int i = 0, n = entities.size; i < n; i++) {
			Entity entity = (Entity) ents[i];
			entity.act(delta);
		}
		entities.end();
		
		//draw all entities
		for (Entity entity : entities) {
			entity.draw();
		}
		
		//draw debug
		for (Entity entity : entities) {
			Item item = entity.item;
			if (item != null) {
				shapeDrawer.setColor(Color.RED);
				shapeDrawer.setDefaultLineWidth(1.0f);
				Rect rect = world.getRect(item);
				shapeDrawer.rectangle(rect.x, rect.y, rect.w, rect.h);
			}
		}
		
		spriteBatch.end();
	}
	
	@Override
	public void resize(int width, int height) {
		viewport.update(width, height, true);
	}
	
	@Override
	public void dispose() {
		spriteBatch.dispose();
		textureAtlas.dispose();
	}
}