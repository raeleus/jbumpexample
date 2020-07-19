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

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.dongbat.jbump.*;
import com.dongbat.jbump.Response.Result;

/**
 * Enemy that can be destroyed by jumping on its head
 */
public class Enemy extends Entity {
    public static final Animation<AtlasRegion> enemy = new Animation<>(1 / 5f, Core.textureAtlas.findRegions("m-enemy"), PlayMode.LOOP);
    public static final Animation<AtlasRegion> squashed = new Animation<>(1 / 30f, Core.textureAtlas.findRegions("m-enemy-squashed"), PlayMode.LOOP);
    public static final float GRAVITY = 3000f;
    public static final float RUN_SPEED = 200f;
    public static final EnemyCollisionFilter ENEMY_COLLISION_FILTER = new EnemyCollisionFilter();
    public float deathTimer;
    public static final float DEATH_TIME = 1f;
    
    public Enemy() {
        animation = enemy;
        gravityY = -GRAVITY;
        bboxX = 0;
        bboxY = 0;
        bboxWidth = 90;
        bboxHeight = 90;
        item = new Item<>(this);
        
        //move left initially
        deltaX = -RUN_SPEED;
    }
    
    @Override
    public void act(float delta) {
        //update animation frame
        animationTime += delta;
    
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
            if (other instanceof Block || other instanceof Enemy) {
                if (collision.normal.x != 0) deltaX *= -1;
                if (collision.normal.y != 0) {
                    deltaY = 0;
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
                Core.entities.removeValue(this, true);
                Core.world.remove(item);
            }
        }
    }
    
    public boolean isDying() {
        return deathTimer > 0;
    }
    
    /**
     * Call when player steps on head to squash the enemy.
     */
    public void die() {
        animation = squashed;
        deltaX = 0;
        deathTimer = DEATH_TIME;
    }
    
    /**
     * Slide on blocks and other enemies.
     */
    public static class EnemyCollisionFilter implements CollisionFilter {
        @Override
        public Response filter(Item item, Item other) {
            if (other.userData instanceof Block || other.userData instanceof  Enemy) return Response.slide;
            return null;
        }
    }
}
