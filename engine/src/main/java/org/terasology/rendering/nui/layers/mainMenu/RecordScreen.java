/*
 * Copyright 2018 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rendering.nui.layers.mainMenu;

import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.ResourceUrn;
import org.terasology.engine.GameEngine;
import org.terasology.engine.modes.StateLoading;
import org.terasology.engine.paths.PathManager;
import org.terasology.game.GameManifest;
import org.terasology.network.NetworkMode;
import org.terasology.recording.RecordAndReplayStatus;
import org.terasology.recording.RecordAndReplayUtils;
import org.terasology.registry.CoreRegistry;
import org.terasology.rendering.nui.WidgetUtil;
import org.terasology.rendering.nui.layers.mainMenu.savedGames.GameInfo;
import org.terasology.rendering.nui.layers.mainMenu.savedGames.GameProvider;
import org.terasology.rendering.nui.widgets.UIButton;

import java.io.File;
import java.nio.file.Path;

/**
 * Screen for the record menu.
 */
public class RecordScreen extends SelectionScreen {

    public static final ResourceUrn ASSET_URI = new ResourceUrn("engine:recordScreen");

    private static final Logger logger = LoggerFactory.getLogger(RecordScreen.class);

    private RecordAndReplayUtils recordAndReplayUtils;

    @Override
    public void initialise() {
        super.initialise();

        initSaveGamePathWidget(PathManager.getInstance().getSavesPath());

        getGameInfos().subscribeSelection((widget, item) -> {
            find("load", UIButton.class).setEnabled(item != null);
            updateDescription(item);
        });

        getGameInfos().subscribe((widget, item) -> loadGame(item));

        WidgetUtil.trySubscribe(this, "load", button -> {
            final GameInfo gameInfo = getGameInfos().getSelection();
            if (gameInfo != null) {
                loadGame(gameInfo);
            }
        });

        WidgetUtil.trySubscribe(this, "close", button -> {
            RecordAndReplayStatus.setCurrentStatus(RecordAndReplayStatus.NOT_ACTIVATED);
            triggerBackAnimation();
        });
    }

    @Override
    public void onOpened() {
        refreshGameInfoList(GameProvider.getSavedGames());
    }

    private void loadGame(GameInfo item) {
        try {
            final GameManifest manifest = item.getManifest();
            copySaveDirectoryToRecordingLibrary(manifest.getTitle());
            recordAndReplayUtils.setGameTitle(manifest.getTitle());
            config.getWorldGeneration().setDefaultSeed(manifest.getSeed());
            config.getWorldGeneration().setWorldTitle(manifest.getTitle());
            CoreRegistry.get(GameEngine.class).changeState(new StateLoading(manifest, NetworkMode.NONE));
        } catch (Exception e) {
            logger.error("Failed to load saved game", e);
            getManager().pushScreen(MessagePopup.ASSET_URI, MessagePopup.class).setMessage("Error Loading Game", e.getMessage());
        }
    }

    private void copySaveDirectoryToRecordingLibrary(String gameTitle) {
        File saveDirectory = new File(PathManager.getInstance().getSavePath(gameTitle).toString());
        Path destinationPath = PathManager.getInstance().getRecordingPath(gameTitle);
        File destDirectory = new File(destinationPath.toString());
        try {
            FileUtils.copyDirectoryStructure(saveDirectory, destDirectory);
        } catch (Exception e) {
            logger.error("Error trying to copy the save directory:", e);
        }
    }

    void setRecordAndReplayUtils(RecordAndReplayUtils recordAndReplayUtils) {
        this.recordAndReplayUtils = recordAndReplayUtils;
    }
}
