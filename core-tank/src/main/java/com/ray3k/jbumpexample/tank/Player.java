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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.dongbat.jbump.*;
import com.dongbat.jbump.Response.Result;

import java.util.ArrayList;

import static com.ray3k.jbumpexample.tank.Core.*;
import static com.ray3k.jbumpexample.tank.Utils.approach360;

public class Player extends Entity {
    public static final Animation<AtlasRegion> tank = new Animation<>(1 / 30f, Core.textureAtlas.findRegions("t-tank"), PlayMode.LOOP);
    public static final Animation<AtlasRegion> turret = new Animation<>(1 / 30f, Core.textureAtlas.findRegions("t-tank-turret"), PlayMode.LOOP);
    public static final Animation<AtlasRegion> bullet = new Animation<>(1 / 30f, Core.textureAtlas.findRegions("t-bullet"), PlayMode.LOOP);
    public static final Sound bulletSound = Gdx.audio.newSound(Gdx.files.internal("bullet.mp3"));
    public static final Sound laserSound = Gdx.audio.newSound(Gdx.files.internal("laser.mp3"));
    public static final Sound hurtSound = Gdx.audio.newSound(Gdx.files.internal("hurt.mp3"));
    public static final PlayerCollisionFilter PLAYER_COLLISION_FILTER = new PlayerCollisionFilter();
    public static final BulletCollisionFilter BULLLET_COLLISION_FILTER = new BulletCollisionFilter();
    public static final Vector2 vector2 = new Vector2();
    public static final Vector3 vector3 = new Vector3();
    public static final Collisions tempCollisions = new Collisions();
    public static final float MOVE_ACCELERATION = 1600f;
    public static final float MOVE_BRAKE = 2600f;
    public static final float MOVE_SPEED = 800f;
    public static final float ROTATION_SPEED = 180f;
    public static final float TURRET_ROTATION_SPEED = 500f;
    public static final float TURRET_ROTATION_SPEED_FIRING = 100f;
    public static final float BULLET_START_DISTANCE = 120f;
    public static final float BULLET_SPEED = 1200f;
    public static final float BULLET_DELAY = .025f;
    public static final float BULLET_PUSH_MAGNITUDE = .25f;
    public float bulletTimer;
    public Turret turretEntity;
    public static final ArrayList<ItemInfo> infos = new ArrayList<>();
    
    public Player() {
        animation = tank;
        bboxX = 15;
        bboxY = 15;
        bboxWidth = 100;
        bboxHeight = 100;
        item = new Item<>(this);
        x = Gdx.graphics.getWidth() / 2;
        y = Gdx.graphics.getHeight() / 2;
        
        entities.add(this);
        world.add(item, x + bboxX, y + bboxY, bboxWidth, bboxHeight);
        
        turretEntity = new Turret();
        entities.add(turretEntity);
    }
    
    @Override
    public void act(float delta) {
        //update animation frame
        animationTime += delta;
        
        //handle movement and input
        boolean left = Gdx.input.isKeyPressed(Keys.LEFT);
        boolean right = Gdx.input.isKeyPressed(Keys.RIGHT);
        boolean up = Gdx.input.isKeyPressed(Keys.UP);
        boolean down = Gdx.input.isKeyPressed(Keys.DOWN);
        float direction = 0f;
        if (left || right || up || down) {
            if (left || right) {
                if (left) {
                    direction = 180f;
                    if (up) direction = 135f;
                    else if (down) direction = 225f;
                } else {
                    direction = 0f;
                    if (up) direction = 45f;
                    else if (down) direction = 315f;
                }
            } else {
                if (up) direction = 90f;
                else if (down) direction = 270f;
            }
            vector2.set(MOVE_SPEED, 0);
            vector2.rotate(direction);
            deltaX = Utils.approach(deltaX, vector2.x, MOVE_ACCELERATION * delta);
            deltaY = Utils.approach(deltaY, vector2.y, MOVE_ACCELERATION * delta);
            rotation = approach360(rotation, direction, ROTATION_SPEED * delta);
        } else {
            deltaX = Utils.approach(deltaX, 0, MOVE_BRAKE * delta);
            deltaY = Utils.approach(deltaY, 0, MOVE_BRAKE * delta);
        }
    
        //update physics
        x += delta * deltaX;
        y += delta * deltaY;
        
        //handle collisions
        Result result = world.move(item, x + bboxX, y + bboxY, PLAYER_COLLISION_FILTER);
        for (int i = 0; i < result.projectedCollisions.size(); i++) {
            Collision collision = result.projectedCollisions.get(i);
            if (collision.other.userData instanceof Enemy) {
                Enemy enemy = (Enemy) collision.other.userData;
                if (!enemy.isDying()) {
                    //ran into enemy: kill the player
                    entities.removeValue(this, true);
                    if (item != null) {
                        world.remove(item);
                        item = null;
                    }
                    hurtSound.play();
                    
                    //remove the turret
                    entities.removeValue(turretEntity, true);
                }
            }
        }
        
        //update position based on collisions
        Rect rect = world.getRect(item);
        if (rect != null) {
            x = rect.x - bboxX;
            y = rect.y - bboxY;
        }
    }
    
    /**
     * Class to render turret graphic and shoot bullets
     */
    public class Turret extends Entity {
        private float width;
        private float height;
        
        public Turret() {
            animation = turret;
            width = turret.getKeyFrames()[0].getRegionWidth();
            height = turret.getKeyFrames()[0].getRegionHeight();
        }
    
        @Override
        public void act(float delta) {
            x = Player.this.x + Player.this.bboxX + Player.this.bboxWidth / 2 - width / 2;
            y = Player.this.y + Player.this.bboxY + Player.this.bboxHeight / 2- height / 2;
            
            boolean leftClick = Gdx.input.isButtonPressed(Buttons.LEFT);
            
            vector3.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(vector3);
            vector2.set(vector3.x, vector3.y);
            vector2.sub(x + width / 2, y + width / 2);
            rotation = approach360(rotation, vector2.angle(), (leftClick ? TURRET_ROTATION_SPEED_FIRING : TURRET_ROTATION_SPEED) * delta);
            
            if (bulletTimer > 0) {
                bulletTimer -= delta;
                if (bulletTimer < 0) bulletTimer = 0;
            }
            
            if (leftClick && bulletTimer == 0) {
                bulletTimer = BULLET_DELAY;
                bulletSound.play();
                
                Bullet bullet = new Bullet();
                vector2.set(BULLET_START_DISTANCE, 0);
                vector2.rotate(rotation);
                bullet.x = x + width / 2 - bullet.bboxWidth / 2 + vector2.x;
                bullet.y = y + height / 2 - bullet.bboxHeight / 2 + vector2.y;
                
                vector2.set(BULLET_SPEED, 0);
                vector2.rotate(rotation);
                bullet.deltaX = vector2.x;
                bullet.deltaY = vector2.y;
    
                entities.add(bullet);
                world.add(bullet.item, bullet.x + bullet.bboxX, bullet.y + bullet.bboxY, bullet.bboxWidth, bullet.bboxHeight);
            }
        }
    
        @Override
        public void draw() {
            super.draw();
            
            //calculate begin point of laser
            vector2.set(BULLET_START_DISTANCE, 0);
            vector2.rotate(rotation);
            float turretX = x + width / 2 + vector2.x;
            float turretY = y + height / 2 + vector2.y;
            
            //calculate end point of laser
            vector2.setLength(800f);
            vector2.add(x + width / 2, y + height /2);
            
            //collide with the closest enemy
            world.querySegmentWithCoords(turretX, turretY, vector2.x, vector2.y,CollisionFilter.defaultFilter, infos);
            if (infos.size() > 0) {
                vector2.set(infos.get(0).x1, infos.get(0).y1);
            }
            
            //draw laser
            shapeDrawer.setColor(Color.GREEN);
            shapeDrawer.line(turretX, turretY, vector2.x, vector2.y);
        }
    }
    
    public class Bullet extends Entity {
        public Bullet() {
            animation = bullet;
            
            bboxX = 0;
            bboxY = 0;
            bboxWidth = bullet.getKeyFrames()[0].getRegionWidth();
            bboxHeight = bullet.getKeyFrames()[0].getRegionHeight();
            
            item = new Item<>(this);
        }
    
        @Override
        public void act(float delta) {
            //update physics
            x += delta * deltaX;
            y += delta * deltaY;
            
            //handle collisions
            Result result = world.move(item, x, y, BULLLET_COLLISION_FILTER);
            for (int i = 0; i < result.projectedCollisions.size(); i++) {
                Collision collision = result.projectedCollisions.get(i);
                if (collision.other.userData instanceof Enemy) {
                    //ran into enemy: kill bullet
                    entities.removeValue(this, true);
                    if (item != null) {
                        world.remove(item);
                        item = null;
                    }
                    
                    Enemy enemy = (Enemy) collision.other.userData;
                    if (!enemy.isDying()) {
                        //enemy is not dead yet: kill it
                        enemy.die();
                        hurtSound.play();
                    } else {
                        //push the enemy
                        enemy.deltaX += deltaX * BULLET_PUSH_MAGNITUDE;
                        enemy.deltaY += deltaY * BULLET_PUSH_MAGNITUDE;
                    }
                }
            }
            
            //update position based on collisions
            Rect rect = world.getRect(item);
            if (rect != null) {
                x = rect.x;
                y = rect.y;
            }
            
            //if outside view
            if (x < camera.position.x - camera.viewportWidth / 2 || x > camera.position.x + camera.viewportWidth / 2 ||
                    y < camera.position.y - camera.viewportHeight / 2 || y > camera.position.y + camera.viewportHeight / 2) {
                //destroy the entity
                entities.removeValue(this, true);
                if (item != null) {
                    world.remove(item);
                    item = null;
                }
            }
        }
    }
    
    public static class PlayerCollisionFilter implements CollisionFilter {
        @Override
        public Response filter(Item item, Item other) {
            if (other.userData instanceof Enemy) return Response.slide;
            else return null;
        }
    }
    
    public static class BulletCollisionFilter implements CollisionFilter {
        @Override
        public Response filter(Item item, Item other) {
            if (other.userData instanceof Enemy) return Response.cross;
            else return null;
        }
    }
}
