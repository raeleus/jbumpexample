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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.dongbat.jbump.*;
import com.dongbat.jbump.Response.Result;

import static com.ray3k.jbumpexample.tank.Core.*;

public class Enemy extends Entity {
    public static final Animation<AtlasRegion> enemy = new Animation<>(1 / 30f, Core.textureAtlas.findRegions("t-fly"), PlayMode.LOOP);
    public static final float MOVE_SPEED = 250f;
    public static final EnemyCollisionFilter ENEMY_COLLISION_FILTER = new EnemyCollisionFilter();
    public float deathTimer;
    public static final float DEATH_TIME = 1f;
    public static final float DEATH_FRICTION = 100f;
    public static final Vector2 vector2 = new Vector2();
    
    public Enemy() {
        animation = enemy;
        bboxX = 25;
        bboxY = 25;
        bboxWidth = 90;
        bboxHeight = 90;
        item = new Item<>(this);
        
        deltaX = -MOVE_SPEED;
        
        entities.add(this);
        world.add(item, x, y, bboxWidth, bboxHeight);
    }
    
    @Override
    public void act(float delta) {
        if (deathTimer <= 0) {
            //update animation frame
            animationTime += delta;
    
            //face player
            vector2.set(player.x, player.y);
            vector2.sub(x, y);
            rotation = vector2.angle();
    
            //move towards player
            vector2.set(MOVE_SPEED, 0);
            vector2.rotate(rotation);
            deltaX = vector2.x;
            deltaY = vector2.y;
        } else {
            vector2.set(deltaX, deltaY);
            vector2.setLength(Utils.approach(vector2.len(), 0, DEATH_FRICTION));
            deltaX = vector2.x;
            deltaY = vector2.y;
        }
    
        //update physics
        deltaX += delta * gravityX;
        deltaY += delta * gravityY;
        x += delta * deltaX;
        y += delta * deltaY;

        //handle collisions
        Result result = Core.world.move(item, x + bboxX, y + bboxY, ENEMY_COLLISION_FILTER);
        for (int i = 0; i < result.projectedCollisions.size(); i++) {
            Collision collision = result.projectedCollisions.get(i);
            Entity other = (Entity) collision.other.userData;
            if (other instanceof Enemy) {
                Enemy enemy = (Enemy) other;
                if (enemy.isDying()) {

                }
            }
        }

        //update position based on collisions
        Rect rect = Core.world.getRect(item);
        x = rect.x - bboxX;
        y = rect.y - bboxY;
    
        //handle death
        if (deathTimer > 0) {
            deathTimer -= delta;
            if (deathTimer <= 0) {
                entities.removeValue(this, true);
                Core.world.remove(item);
            }
        }
    }
    
    public boolean isDying() {
        return deathTimer > 0;
    }
    
    public void die() {
        color = Color.DARK_GRAY;
        deltaX = 0;
        deltaY = 0;
        deathTimer = DEATH_TIME;
    }
    
    public static class EnemyCollisionFilter implements CollisionFilter {
        @Override
        public Response filter(Item item, Item other) {
            if (other.userData instanceof  Enemy) return Response.cross;
            return null;
        }
    }
}
