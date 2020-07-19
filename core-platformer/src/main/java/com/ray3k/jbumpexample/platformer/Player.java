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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.dongbat.jbump.*;
import com.dongbat.jbump.Response.Result;

import static com.ray3k.jbumpexample.platformer.Core.*;

public class Player extends Entity {
    public static final Animation<AtlasRegion> walk = new Animation<>(1 / 30f, textureAtlas.findRegions("m-player-walk"), PlayMode.LOOP);
    public static final Animation<AtlasRegion> stand = new Animation<>(1 / 30f, textureAtlas.findRegions("m-player-stand"), PlayMode.LOOP);
    public static final Animation<AtlasRegion> jump = new Animation<>(1 / 30f, textureAtlas.findRegions("m-player-jump"), PlayMode.LOOP);
    public static final Animation<AtlasRegion> wall = new Animation<>(1 / 30f, textureAtlas.findRegions("m-player-wall"), PlayMode.LOOP);
    public static final Sound jumpSound = Gdx.audio.newSound(Gdx.files.internal("jump.mp3"));
    public static final Sound hurtSound = Gdx.audio.newSound(Gdx.files.internal("hurt.mp3"));
    public static final Sound killSound = Gdx.audio.newSound(Gdx.files.internal("kill.mp3"));
    public static final PlayerCollisionFilter PLAYER_COLLISION_FILTER = new PlayerCollisionFilter();
    public static final Collisions tempCollisions = new Collisions();
    public static final float FRICTION = 250f;
    public static final float RUN_ACCELERATION = 1800f;
    public static final float RUN_SPEED = 800f;
    public static final float JUMP_SPEED = 1200f;
    public static final float BOUNCE_SPEED = 800f;
    public static final float GRAVITY = 3000f;
    public static final float JUMP_MAX_TIME = .25f;
    private float jumpTime;
    private boolean jumping;
    
    public Player() {
        animation = stand;
        gravityY = -GRAVITY;
        bboxX = 30;
        bboxY = 15;
        bboxWidth = 70;
        bboxHeight = 130;
        item = new Item<>(this);
    }
    
    @Override
    public void act(float delta) {
        //update animation frame
        animationTime += delta;
        
        //handle movement and input
        deltaX = Utils.approach(deltaX, 0, FRICTION * delta);
        boolean left = Gdx.input.isKeyPressed(Keys.LEFT);
        boolean right = Gdx.input.isKeyPressed(Keys.RIGHT);
        boolean up = Gdx.input.isKeyPressed(Keys.UP);
        boolean upJustPressed = Gdx.input.isKeyJustPressed(Keys.UP);
        if (right) {
            animation = walk;
            flipX = false;
            deltaX = Utils.approach(deltaX, RUN_SPEED, RUN_ACCELERATION * delta);
        } else if (left) {
            animation = walk;
            flipX = true;
            deltaX = Utils.approach(deltaX, -RUN_SPEED, RUN_ACCELERATION * delta);
        } else {
            animation = stand;
            deltaX = Utils.approach(deltaX, 0f, RUN_ACCELERATION * delta);
        }
        
        if (!up) jumping = false;
        if (upJustPressed) {
            //check if the player is on ground.
            world.project(item, x + bboxX, y + bboxY, bboxWidth, bboxHeight, x + bboxX, y + bboxY - .1f,
                    PLAYER_COLLISION_FILTER, tempCollisions);
            if (tempCollisions.size() > 0) {
                //start jump
                jumping = true;
                jumpSound.play();
            }
        }
        
        //continue accelerating upwards while player holds jump key
        if (up && jumping && jumpTime < JUMP_MAX_TIME) {
            deltaY = JUMP_SPEED;
            jumpTime += delta;
        }
    
        //update physics
        deltaX += delta * gravityX;
        deltaY += delta * gravityY;
        x += delta * deltaX;
        y += delta * deltaY;
        
        //handle collisions
        boolean inAir = true;
        boolean hitWall = false;
        Result result = world.move(item, x + bboxX, y + bboxY, PLAYER_COLLISION_FILTER);
        for (int i = 0; i < result.projectedCollisions.size(); i++) {
            Collision collision = result.projectedCollisions.get(i);
            if (collision.other.userData instanceof Block) {
                if (collision.normal.x != 0) {
                    //hit a wall
                    deltaX = 0;
                    hitWall = true;
                }
                if (collision.normal.y != 0) {
                    //hit ceiling or floor
                    deltaY = 0;
                    jumpTime = JUMP_MAX_TIME;
                    
                    if (collision.normal.y == 1) {
                        //hit floor
                        jumpTime = 0f;
                        jumping = false;
                        inAir = false;
                    }
                }
            } else if (collision.other.userData instanceof Enemy) {
                Enemy enemy = (Enemy) collision.other.userData;
                if (!enemy.isDying()) {
                    if (collision.normal.y == 1 && !collision.overlaps) {
                        //landed on enemy: bounce
                        deltaY = BOUNCE_SPEED;
                        killSound.play();
                        enemy.die();
                    } else {
                        //ran into enemy: kill the player
                        entities.removeValue(this, true);
                        world.remove(item);
                        hurtSound.play();
                    }
                }
            }
        }
        
        //update position based on collisions
        Rect rect = world.getRect(item);
        if (rect != null) {
            x = rect.x - bboxX;
            y = rect.y - bboxY;
        }
        
        //adjust animation based on collisions
        if (inAir) animation = jump;
        else if (hitWall) {
            animation = wall;
        }
        
        //update camera
        camera.position.set(x, y, 0);
    }
    
    /**
     * Slide on blocks, detect collisions with enemies
     */
    public static class PlayerCollisionFilter implements CollisionFilter {
        @Override
        public Response filter(Item item, Item other) {
            if (other.userData instanceof Block) return Response.slide;
            else if (other.userData instanceof Enemy) return Response.cross;
            return null;
        }
    }
}
