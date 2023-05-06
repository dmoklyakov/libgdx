package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.graphics.g2d.msdf.MsdfShader;

public interface MsdfShaderProvider {
    MsdfShader getShader();
}

class DefaultMsdfShaderProvider implements MsdfShaderProvider {
    private MsdfShader shader = null;

    @Override
    public MsdfShader getShader() {
        if (shader == null) {
            shader = new MsdfShader();

            Gdx.app.addLifecycleListener(new LifecycleListener() {
                @Override
                public void pause() {
                }

                @Override
                public void resume() {
                }

                @Override
                public void dispose() {
                    shader.dispose();
                    shader = null;
                }
            });
        }
        return shader;
    }
}