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
package com.ray3k.jbumpexample.platformer;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
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
	public static final String MAP =
			        "+--------------------------------------------------------------------------------------------------+\n" +
					"+------------------------+----------------------------------------------------------------------++-+\n" +
					"+------------------------+------------------------------------------------------------------++-----+\n" +
					"+-++--++-----------------+----------------+-+-+-+-+-+-----------------------------------++---------+\n" +
					"+--------------e--e------+--------------------------------+e-e-e-+------------------++-------------+\n" +
					"+-----------+++++++------+------++------------------------++++++++--------------++-----------------+\n" +
					"+-----------------+------+------++----------------------------------------++-----------------------+\n" +
					"+-----------------+-------------++-----------------------------------------------------------------+\n" +
					"+p----------------+----e-e-e----++--------------------e-----e----------e-----e----e----------------+\n" +
					"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++";
	public static float TILE_DIMENSION = 100f;

	@Override
	public void create() {
		spriteBatch = new SpriteBatch();
		textureAtlas = new TextureAtlas("textures.atlas");
		camera = new OrthographicCamera();
		viewport = new ExtendViewport(800, 800, camera);
		entities = new SnapshotArray<>();
		world = new World<>(TILE_DIMENSION);
		shapeDrawer = new ShapeDrawer(spriteBatch, textureAtlas.findRegion("white"));
		
		//load map and spawn entities
		loadMap();
	}
	
	private void loadMap() {
		for (Entity entity : entities) {
			if (entity.item != null) world.remove(entity.item);
		}
		entities.clear();
		
		String[] lines = MAP.split("\n");
		for (int j = 0; j < lines.length; j++) {
			String line = lines[j];
			for (int i = 0; i < line.length(); i++) {
				Entity entity = null;
				if (line.charAt(i) == '+') entity = new Block();
				else if (line.charAt(i) == 'p') entity = new Player();
				else if (line.charAt(i) == 'e') entity = new Enemy();
				
				if (entity != null) {
					entities.add(entity);
					entity.x = i * TILE_DIMENSION;
					entity.y = (lines.length - j) * TILE_DIMENSION;
					if (entity.item != null) {
						world.add(entity.item, entity.x + entity.bboxX, entity.y + entity.bboxY, entity.bboxWidth, entity.bboxHeight);
					}
				}
			}
		}
	}

	@Override
	public void render() {
		//allow player to reset the game
		if (Gdx.input.isKeyJustPressed(Keys.F5)) {
			loadMap();
		}
		
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		//prepare batch
		viewport.apply();
		spriteBatch.setProjectionMatrix(camera.combined);
		spriteBatch.begin();
		
		//call logic on all entities
		float delta = Gdx.graphics.getDeltaTime();
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
		viewport.update(width, height);
	}
	
	@Override
	public void dispose() {
		spriteBatch.dispose();
		textureAtlas.dispose();
	}
}