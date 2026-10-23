package wartech.render;

//  Tactical viewport with terrain tiles, fog of war, entities, effects, overlays
// It's cumbersome AF but it's after midnight and I'm going to sleep, this can wait.
// Expect to find this comment three years later having done squat about this monstrosity.

import wartech.building.Building;
import wartech.building.BuildingType;
import wartech.combat.Effect;
import wartech.core.Camera;
import wartech.core.Faction;
import wartech.core.Memory;
import wartech.core.Surface;
import wartech.core.WarTech;
import wartech.map.GameMap;
import wartech.unit.Unit;
import wartech.unit.UnitType;
import static wartech.render.RenderShared.*;
import static wartech.render.Renderer.*;

public final class ViewportRenderer {
    private final Surface screen;

    public ViewportRenderer(Surface screen) {
        this.screen = screen;
    }

    public void renderViewport(GameMap map, Camera camera, WarTech game) {
        double zoom = camera.zoom;
        int tileDrawSize = (int) (GameMap.TILE_SIZE * zoom);

        int startTileX = Math.max(0, (int) (camera.x / GameMap.TILE_SIZE));
        int startTileY = Math.max(0, (int) (camera.y / GameMap.TILE_SIZE));
        int endTileX = Math.min(map.width - 1, (int) ((camera.x + camera.getWorldViewWidth()) / GameMap.TILE_SIZE) + 1);
        int endTileY = Math.min(map.height - 1, (int) ((camera.y + camera.getWorldViewHeight()) / GameMap.TILE_SIZE) + 1);

        // Terrain first
        for (int ty = startTileY; ty <= endTileY; ty++) {
            int sy = camera.worldToScreenY(ty * GameMap.TILE_SIZE);
            int nextSy = camera.worldToScreenY((ty + 1) * GameMap.TILE_SIZE);
            int drawH = Math.max(1, nextSy - sy);

            for (int tx = startTileX; tx <= endTileX; tx++) {
                int idx = map.index(tx, ty);
                int sx = camera.worldToScreenX(tx * GameMap.TILE_SIZE);
                int nextSx = camera.worldToScreenX((tx + 1) * GameMap.TILE_SIZE);
                int drawW = Math.max(1, nextSx - sx);

                // Fog is black when unexplored, otherwise you sort of see the terrain. Needs a tweak
                if (!map.explored[idx]) {
                    screen.fillRect(Math.max(0, sx), Math.max(0, sy), drawW, drawH, AssetData.COLOR_FOG_UNEXPLORED);
                } else {
                    blitTileBlended(map, tx, ty, sx, sy, drawW, drawH, !map.visible[idx]);
                }
            }
        }

        // Scrap
        for (int i = 0; i < game.wreckCount; i++) {
            var wrk = game.wrecks[i];
            if (!wrk.active) continue;
            int wtx = (int) (wrk.x / GameMap.TILE_SIZE);
            int wty = (int) (wrk.y / GameMap.TILE_SIZE);
            if (!map.inBounds(wtx, wty) || !map.visible[map.index(wtx, wty)]) continue;
            int sx = camera.worldToScreenX(wrk.x - GameMap.TILE_SIZE / 2.0);
            int sy = camera.worldToScreenY(wrk.y - GameMap.TILE_SIZE / 2.0);
            int nextSx = camera.worldToScreenX(wrk.x + GameMap.TILE_SIZE / 2.0);
            int nextSy = camera.worldToScreenY(wrk.y + GameMap.TILE_SIZE / 2.0);
            int drawW = Math.max(1, nextSx - sx);
            int drawH = Math.max(1, nextSy - sy);
            if (sx >= -drawW && sx < VIEW_WIDTH && sy >= -drawH && sy < SCREEN_HEIGHT) {
                blitTile(sx, sy, drawW, drawH, AssetData.WRECKAGE);
            }
        }

        // Buildings
        for (int i = 0; i < game.buildingCount; i++) {
            var b = game.buildings[i];
            if (b.isDestroyed()) continue;
            int sx = camera.worldToScreenX(b.pixelX);
            int sy = camera.worldToScreenY(b.pixelY);
            int nextSx = camera.worldToScreenX(b.pixelX + b.type.widthTiles * GameMap.TILE_SIZE);
            int nextSy = camera.worldToScreenY(b.pixelY + b.type.heightTiles * GameMap.TILE_SIZE);
            int bw = Math.max(1, nextSx - sx);
            int bh = Math.max(1, nextSy - sy);
            int btx = (b.pixelX + (b.type.widthTiles * GameMap.TILE_SIZE) / 2) / GameMap.TILE_SIZE;
            int bty = (b.pixelY + (b.type.heightTiles * GameMap.TILE_SIZE) / 2) / GameMap.TILE_SIZE;
            boolean bVisible = (b.faction == Faction.PLAYER)
                || (map.inBounds(btx, bty) && map.visible[map.index(btx, bty)]);
            if (!bVisible) continue;

            if (sx + bw >= 0 && sx < VIEW_WIDTH && sy + bh >= 0 && sy < SCREEN_HEIGHT) {
                renderBuilding(b, sx, sy, bw, bh, game.gameTime, game, camera);
            }
        }

        // Units
        for (int i = 0; i < game.unitCount; i++) {
            var u = game.units[i];
            if (u.isDestroyed()) continue;
            int sx = camera.worldToScreenX(u.x);
            int sy = camera.worldToScreenY(u.y);

            int tx = (int) (u.x / GameMap.TILE_SIZE);
            int ty = (int) (u.y / GameMap.TILE_SIZE);
            boolean isVisible = (u.faction == Faction.PLAYER) || (map.inBounds(tx, ty) && map.visible[map.index(tx, ty)]);

            if (isVisible && sx >= -tileDrawSize && sx < VIEW_WIDTH + tileDrawSize && sy >= -tileDrawSize && sy < SCREEN_HEIGHT + tileDrawSize) {
                renderUnit(u, sx, sy, game.gameTime, camera, game);
            }
        }

        // Projectiles
        for (int i = 0; i < game.projectileCount; i++) {
            var p = game.projectiles[i];
            if (!p.active) continue;
            int ptx = (int) (p.x / GameMap.TILE_SIZE);
            int pty = (int) (p.y / GameMap.TILE_SIZE);
            if (!map.inBounds(ptx, pty) || !map.visible[map.index(ptx, pty)]) continue;
            int sx = camera.worldToScreenX(p.x);
            int sy = camera.worldToScreenY(p.y);
            if (sx >= 0 && sx < VIEW_WIDTH && sy >= 0 && sy < SCREEN_HEIGHT) {
                int col = p.isRocket ? Palette.WHITE : 0xFF20FFFF;
                int pSize = Math.max(2, (int) (5 * zoom));
                screen.fillRect(sx - pSize / 2, sy - pSize / 2, pSize, pSize, col);
            }
        }

        // FX
        for (int i = 0; i < game.effectCount; i++) {
            var fx = game.effects[i];
            if (!fx.active) continue;
            int ftx = (int) (fx.x / GameMap.TILE_SIZE);
            int fty = (int) (fx.y / GameMap.TILE_SIZE);
            if (!map.inBounds(ftx, fty) || !map.visible[map.index(ftx, fty)]) continue;
            int sx = camera.worldToScreenX(fx.x);
            int sy = camera.worldToScreenY(fx.y);
            if (sx >= -tileDrawSize && sx < VIEW_WIDTH + tileDrawSize && sy >= -tileDrawSize && sy < SCREEN_HEIGHT + tileDrawSize) {
                renderEffect(fx, sx, sy, zoom);
            }
        }

        // Busy building foundation overlay
        if (game.placingBuilding != null) {
            int mx = game.mouseX;
            int my = game.mouseY;
            if (mx < VIEW_WIDTH) {
                double wx = camera.screenToWorldX(mx);
                double wy = camera.screenToWorldY(my);
                int gtx = (int) (wx / GameMap.TILE_SIZE);
                int gty = (int) (wy / GameMap.TILE_SIZE);
                boolean valid = map.canBuildAt(gtx, gty, game.placingBuilding, true,
                    game.buildings, game.buildingCount, game.units, game.unitCount);

                int gsx = camera.worldToScreenX(gtx * GameMap.TILE_SIZE);
                int gsy = camera.worldToScreenY(gty * GameMap.TILE_SIZE);
                int nextGsx = camera.worldToScreenX((gtx + game.placingBuilding.widthTiles) * GameMap.TILE_SIZE);
                int nextGsy = camera.worldToScreenY((gty + game.placingBuilding.heightTiles) * GameMap.TILE_SIZE);
                int gw = Math.max(1, nextGsx - gsx);
                int gh = Math.max(1, nextGsy - gsy);

                int bufSx = camera.worldToScreenX((gtx - 1) * GameMap.TILE_SIZE);
                int bufSy = camera.worldToScreenY((gty - 1) * GameMap.TILE_SIZE);
                int bufEx = camera.worldToScreenX((gtx + game.placingBuilding.widthTiles + 1) * GameMap.TILE_SIZE);
                int bufEy = camera.worldToScreenY((gty + game.placingBuilding.heightTiles + 1) * GameMap.TILE_SIZE);
                int bufCol = valid ? 0xFF2E7A38 : 0xFF2E2E7A; // ABGR: dim green / dim red
                screen.drawLine(bufSx, bufSy, bufEx, bufSy, bufCol);
                screen.drawLine(bufSx, bufEy, bufEx, bufEy, bufCol);
                screen.drawLine(bufSx, bufSy, bufSx, bufEy, bufCol);
                screen.drawLine(bufEx, bufSy, bufEx, bufEy, bufCol);

                if (game.placingBuilding.deploysUnitsAtExit()) {
                    int exitSy = camera.worldToScreenY((gty + game.placingBuilding.heightTiles) * GameMap.TILE_SIZE);
                    int exitEy = camera.worldToScreenY((gty + game.placingBuilding.heightTiles + 1) * GameMap.TILE_SIZE);
                    blendRect(screen, gsx, exitSy, gw, Math.max(1, exitEy - exitSy), 0x55FFD040);
                }

                int tint = valid ? 0x8800FF00 : 0x880000FF; // Green or Red
                blendRect(screen, gsx, gsy, gw, gh, tint);
            }
        }

        // Drag box
        if (game.isDragging && game.dragStartX < VIEW_WIDTH) {
            int x0 = Math.min(game.dragStartX, game.mouseX);
            int y0 = Math.min(game.dragStartY, game.mouseY);
            int x1 = Math.max(game.dragStartX, game.mouseX);
            int y1 = Math.max(game.dragStartY, game.mouseY);

            int boxCol = Palette.GREEN_PURE;
            screen.drawLine(x0, y0, x1, y0, boxCol);
            screen.drawLine(x1, y0, x1, y1, boxCol);
            screen.drawLine(x1, y1, x0, y1, boxCol);
            screen.drawLine(x0, y1, x0, y0, boxCol);
        }

        // Building rally point
        if (game != null && game.selectedBuilding != null && game.selectedBuilding.faction == Faction.PLAYER && game.selectedBuilding.hasRallyPoint) {
            renderRallyPoint(game.selectedBuilding, game, camera);
        }
    }

    private void renderRallyPoint(Building b, WarTech game, Camera camera) {
        if (b == null || !b.hasRallyPoint) return;

        double spawnWx = b.exitSpawnX();
        double spawnWy = b.exitSpawnY();
        double rallyWx = b.rallyX;
        double rallyWy = b.rallyY;

        int sx0 = camera.worldToScreenX(spawnWx);
        int sy0 = camera.worldToScreenY(spawnWy);
        int sx1 = camera.worldToScreenX(rallyWx);
        int sy1 = camera.worldToScreenY(rallyWy);

        double dx = sx1 - sx0;
        double dy = sy1 - sy0;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist > 4.0) {
            double dirX = dx / dist;
            double dirY = dy / dist;
            double dashLen = 8.0;
            double gapLen = 6.0;
            double period = dashLen + gapLen;
            double offset = (game.gameTime * 28.0) % period;

            for (double t = offset; t < dist; t += period) {
                double t0 = t;
                double t1 = Math.min(dist, t + dashLen);
                int lx0 = (int) (sx0 + dirX * t0);
                int ly0 = (int) (sy0 + dirY * t0);
                int lx1 = (int) (sx0 + dirX * t1);
                int ly1 = (int) (sy0 + dirY * t1);

                if ((lx0 >= 0 || lx1 >= 0) && (lx0 < VIEW_WIDTH || lx1 < VIEW_WIDTH) &&
                    (ly0 >= 0 || ly1 >= 0) && (ly0 < SCREEN_HEIGHT || ly1 < SCREEN_HEIGHT)) {
                    if (lx0 < VIEW_WIDTH || lx1 < VIEW_WIDTH) {
                        screen.drawLine(Math.max(0, Math.min(VIEW_WIDTH - 1, lx0)),
                                        Math.max(0, Math.min(SCREEN_HEIGHT - 1, ly0)),
                                        Math.max(0, Math.min(VIEW_WIDTH - 1, lx1)),
                                        Math.max(0, Math.min(SCREEN_HEIGHT - 1, ly1)),
                                        Palette.GOLD_LIGHT);
                    }
                }
            }
        }

        if (sx1 >= -32 && sx1 < VIEW_WIDTH + 32 && sy1 >= -32 && sy1 < SCREEN_HEIGHT + 32) {
            double zoom = camera.zoom;
            int groundR = Math.max(4, (int) (12 * zoom));

            int ringCol = Palette.GREEN;
            screen.drawLine(Math.max(0, sx1 - groundR), Math.max(0, sy1), Math.min(VIEW_WIDTH - 1, sx1 - groundR + 4), Math.max(0, sy1), ringCol);
            screen.drawLine(Math.max(0, sx1 + groundR - 4), Math.max(0, sy1), Math.min(VIEW_WIDTH - 1, sx1 + groundR), Math.max(0, sy1), ringCol);
            screen.drawLine(Math.max(0, sx1), Math.max(0, sy1 - groundR), Math.max(0, sx1), Math.min(SCREEN_HEIGHT - 1, sy1 - groundR + 4), ringCol);
            screen.drawLine(Math.max(0, sx1), Math.max(0, sy1 + groundR - 4), Math.max(0, sx1), Math.min(SCREEN_HEIGHT - 1, sy1 + groundR), ringCol);

            int pipSize = ((int) (game.gameTime * 6.0) % 2 == 0) ? 3 : 2;
            int pipX = Math.max(0, Math.min(VIEW_WIDTH - 1 - pipSize, sx1 - pipSize / 2));
            int pipY = Math.max(0, Math.min(SCREEN_HEIGHT - 1 - pipSize, sy1 - pipSize / 2));
            screen.fillRect(pipX, pipY, pipSize, pipSize, 0xFF00FFFF);

            int poleH = Math.max(14, (int) (26 * zoom));
            int poleTopY = Math.max(14, sy1 - poleH);
            if (sx1 >= 0 && sx1 < VIEW_WIDTH) {
                screen.drawLine(sx1, sy1, sx1, poleTopY, Palette.GRAY_LIGHT);
                screen.fillRect(Math.max(0, sx1 - 1), Math.max(0, poleTopY - 2), 3, 3, Palette.BLUE_SKY); // finial
            }

            int flagW = Math.max(8, (int) (14 * zoom));
            int flagH = Math.max(6, (int) (10 * zoom));
            int flagColor = Palette.GOLD; // Vanguard gold
            int flagTrim = Palette.WHITE;

            for (int r = 0; r < flagH; r++) {
                int rowW = flagW - (r * flagW / flagH);
                if (rowW > 0 && sx1 + 1 < VIEW_WIDTH && poleTopY + r < SCREEN_HEIGHT) {
                    screen.fillRect(sx1 + 1, poleTopY + r, Math.min(rowW, VIEW_WIDTH - 1 - (sx1 + 1)), 1, flagColor);
                }
            }
            if (sx1 + 1 + flagW < VIEW_WIDTH) {
                screen.drawLine(sx1 + 1, poleTopY, sx1 + 1 + flagW, poleTopY, flagTrim);
                screen.drawLine(sx1 + 1 + flagW, poleTopY, sx1 + 1, poleTopY + flagH, flagTrim);
            }

            if (sx1 - 14 >= 0 && sx1 + 18 < VIEW_WIDTH && poleTopY - 11 >= 0) {
                BitmapFont.drawTextWithShadow(screen, "RALLY", sx1 - 14, poleTopY - 11, Palette.GREEN, 1);
            }
        }
    }

    private int tileVariant(int tx, int ty) {
        int h = tx * 374761393 + ty * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return (h ^ (h >>> 16)) & 3;
    }

    private int[] sandTile(int v) {
        return switch (v) {
            case 0 -> AssetData.SAND_0;
            case 1 -> AssetData.SAND_1;
            case 2 -> AssetData.SAND_2;
            default -> AssetData.SAND_3;
        };
    }

    private int[] rockTile(int v) {
        return switch (v) {
            case 0 -> AssetData.BEDROCK_0;
            case 1 -> AssetData.BEDROCK_1;
            case 2 -> AssetData.BEDROCK_2;
            default -> AssetData.BEDROCK_3;
        };
    }

    // 0 = sand, 1 = bedrock, 2 = other
    private int baseKind(GameMap map, int tx, int ty) {
        if (!map.inBounds(tx, ty)) return 2;
        byte t = map.terrain[map.index(tx, ty)];
        if (t == GameMap.TERRAIN_BEDROCK) return 1;
        if (t == GameMap.TERRAIN_SAND) return 0;
        return 2;
    }

    private int[] cliffTile(int v) {
        return switch (v) {
            case 0 -> AssetData.CLIFF_0;
            case 1 -> AssetData.CLIFF_1;
            case 2 -> AssetData.CLIFF_2;
            default -> AssetData.CLIFF_3;
        };
    }

    private boolean isCliffTile(GameMap map, int tx, int ty) {
        return map.inBounds(tx, ty) && map.terrain[map.index(tx, ty)] == GameMap.TERRAIN_CLIFF;
    }

    private int[] baseTile(GameMap map, int tx, int ty) {
        byte t = map.terrain[map.index(tx, ty)];
        if (t == GameMap.TERRAIN_CLIFF) return cliffTile(tileVariant(tx, ty));
        if (t == GameMap.TERRAIN_RUIN) return AssetData.PRECURSOR_RUIN;
        int v = tileVariant(tx, ty);
        return (t == GameMap.TERRAIN_BEDROCK) ? rockTile(v) : sandTile(v);
    }

    private int[] coastTile(int set, int mask) {
        if (set == 0) {
            return switch (mask) {
                case 1 -> AssetData.COAST0_M01;
                case 2 -> AssetData.COAST0_M02;
                case 3 -> AssetData.COAST0_M03;
                case 4 -> AssetData.COAST0_M04;
                case 5 -> AssetData.COAST0_M05;
                case 6 -> AssetData.COAST0_M06;
                case 7 -> AssetData.COAST0_M07;
                case 8 -> AssetData.COAST0_M08;
                case 9 -> AssetData.COAST0_M09;
                case 10 -> AssetData.COAST0_M10;
                case 11 -> AssetData.COAST0_M11;
                case 12 -> AssetData.COAST0_M12;
                case 13 -> AssetData.COAST0_M13;
                case 14 -> AssetData.COAST0_M14;
                default -> AssetData.COAST0_M15;
            };
        }
        return switch (mask) {
            case 1 -> AssetData.COAST1_M01;
            case 2 -> AssetData.COAST1_M02;
            case 3 -> AssetData.COAST1_M03;
            case 4 -> AssetData.COAST1_M04;
            case 5 -> AssetData.COAST1_M05;
            case 6 -> AssetData.COAST1_M06;
            case 7 -> AssetData.COAST1_M07;
            case 8 -> AssetData.COAST1_M08;
            case 9 -> AssetData.COAST1_M09;
            case 10 -> AssetData.COAST1_M10;
            case 11 -> AssetData.COAST1_M11;
            case 12 -> AssetData.COAST1_M12;
            case 13 -> AssetData.COAST1_M13;
            case 14 -> AssetData.COAST1_M14;
            default -> AssetData.COAST1_M15;
        };
    }

    private int[] cornerTile(int set, int diag) {
        if (set == 0) {
            return switch (diag) {
                case 0 -> AssetData.CORN0_NE;
                case 1 -> AssetData.CORN0_SE;
                case 2 -> AssetData.CORN0_SW;
                default -> AssetData.CORN0_NW;
            };
        }
        return switch (diag) {
            case 0 -> AssetData.CORN1_NE;
            case 1 -> AssetData.CORN1_SE;
            case 2 -> AssetData.CORN1_SW;
            default -> AssetData.CORN1_NW;
        };
    }

    private void blitTileBlended(GameMap map, int tx, int ty, int sx, int sy, int drawW, int drawH,
                                 boolean shade) {
        int[] base = baseTile(map, tx, ty);
        int idx = map.index(tx, ty);
        boolean isCliff = map.terrain[idx] == GameMap.TERRAIN_CLIFF;
        boolean nCliff = isCliffTile(map, tx, ty - 1);
        boolean wCliff = isCliffTile(map, tx - 1, ty);
        boolean hasRelief = isCliff;
        boolean hasShadow = !isCliff && (nCliff || wCliff);
        boolean deferredFog = shade && (hasRelief || hasShadow);
        boolean tileShade = shade && !deferredFog;

        blitTile(sx, sy, drawW, drawH, base, tileShade);
        if (hasRelief) {
            shadeCliffRelief(sx, sy, drawW, drawH,
                !isCliffTile(map, tx, ty - 1), !isCliffTile(map, tx - 1, ty),
                !isCliffTile(map, tx, ty + 1), !isCliffTile(map, tx + 1, ty), tileShade);
        } else if (hasShadow) {
            shadeCliffShadow(sx, sy, drawW, drawH, nCliff, wCliff, tileShade);
        }
        if (baseKind(map, tx, ty) == 0) {
            int mask = 0;
            if (baseKind(map, tx, ty - 1) == 1) mask |= 1;
            if (baseKind(map, tx + 1, ty) == 1) mask |= 2;
            if (baseKind(map, tx, ty + 1) == 1) mask |= 4;
            if (baseKind(map, tx - 1, ty) == 1) mask |= 8;
            int set = tileVariant(tx, ty) & 1;
            if (mask != 0) {
                blitOverlay(sx, sy, drawW, drawH, coastTile(set, mask), tileShade);
            } else {
                if (baseKind(map, tx + 1, ty - 1) == 1) blitOverlay(sx, sy, drawW, drawH, cornerTile(set, 0), tileShade);
                if (baseKind(map, tx + 1, ty + 1) == 1) blitOverlay(sx, sy, drawW, drawH, cornerTile(set, 1), tileShade);
                if (baseKind(map, tx - 1, ty + 1) == 1) blitOverlay(sx, sy, drawW, drawH, cornerTile(set, 2), tileShade);
                if (baseKind(map, tx - 1, ty - 1) == 1) blitOverlay(sx, sy, drawW, drawH, cornerTile(set, 3), tileShade);
            }
        }

        if (map.visible[idx]) {
            if (map.crystals[idx] > 200) {
                blitOverlay(sx, sy, drawW, drawH, AssetData.CRYSTAL_RICH, tileShade);
            } else if (map.crystals[idx] > 0) {
                blitOverlay(sx, sy, drawW, drawH, AssetData.CRYSTAL_SPARSE, tileShade);
            }
            if (map.geode[idx] == GameMap.GEODE_DORMANT) {
                blitOverlay(sx, sy, drawW, drawH, AssetData.GEODE_DORMANT, tileShade);
            } else if (map.geode[idx] == GameMap.GEODE_BREACHED) {
                blitOverlay(sx, sy, drawW, drawH, AssetData.GEODE_BREACHED, tileShade);
            }
        }

        if (deferredFog) {
            fogTileRect(sx, sy, drawW, drawH);
        }
    }

    private void fogTileRect(int sx, int sy, int drawW, int drawH) {
        int x0 = Math.max(0, sx);
        int y0 = Math.max(0, sy);
        int x1 = Math.min(VIEW_WIDTH, sx + drawW);
        int y1 = Math.min(SCREEN_HEIGHT, sy + drawH);
        if (x0 >= x1 || y0 >= y1) return;

        int baseAddr = screen.base;
        int stride = screen.width;
        for (int y = y0; y < y1; y++) {
            int rowOffset = baseAddr + (y * stride + x0) * 4;
            for (int x = x0; x < x1; x++) {
                Memory.put32(rowOffset, shadeFogColor(Memory.get32(rowOffset)));
                rowOffset += 4;
            }
        }
    }

    private void shadeCliffRelief(int sx, int sy, int drawW, int drawH,
            boolean nOpen, boolean wOpen, boolean sOpen, boolean eOpen, boolean shade) {
        if (!nOpen && !wOpen && !sOpen && !eOpen) return;
        int x0 = Math.max(0, sx);
        int y0 = Math.max(0, sy);
        int x1 = Math.min(VIEW_WIDTH, sx + drawW);
        int y1 = Math.min(SCREEN_HEIGHT, sy + drawH);
        if (x0 >= x1 || y0 >= y1) return;
        int rimH = Math.max(1, drawH / 32);
        int rimW = Math.max(1, drawW / 32);
        int wallH = Math.max(2, drawH * 3 / 16);
        int wallW = Math.max(2, drawW * 3 / 16);
        int baseAddr = screen.base;
        int stride = screen.width;
        for (int y = y0; y < y1; y++) {
            int dy = y - sy;
            int rowOffset = baseAddr + (y * stride + x0) * 4;
            for (int x = x0; x < x1; x++) {
                int dx = x - sx;
                int col = Memory.get32(rowOffset);
                int r = col & 0xFF;
                int g = (col >> 8) & 0xFF;
                int b = (col >> 16) & 0xFF;
                if (nOpen && dy < rimH) {
                    int add = (46 * (rimH - dy)) / rimH;
                    r = Math.min(255, r + add);
                    g = Math.min(255, g + (add * 9 / 10));
                    b = Math.min(255, b + (add * 8 / 10));
                }
                if (wOpen && dx < rimW) {
                    int add = (46 * (rimW - dx)) / rimW;
                    r = Math.min(255, r + add);
                    g = Math.min(255, g + (add * 9 / 10));
                    b = Math.min(255, b + (add * 8 / 10));
                }
                if (sOpen && dy >= drawH - wallH) {
                    int d = drawH - 1 - dy;
                    int k = (d == 0) ? 78 : (60 * (wallH - d)) / wallH;
                    r = (r * (100 - k)) / 100;
                    g = (g * (100 - k)) / 100;
                    b = (b * (100 - k)) / 100;
                }
                if (eOpen && dx >= drawW - wallW) {
                    int d = drawW - 1 - dx;
                    int k = (d == 0) ? 78 : (60 * (wallW - d)) / wallW;
                    r = (r * (100 - k)) / 100;
                    g = (g * (100 - k)) / 100;
                    b = (b * (100 - k)) / 100;
                }
                int out = Palette.BLACK | (b << 16) | (g << 8) | r;
                Memory.put32(rowOffset, shade ? shadeFogColor(out) : out);
                rowOffset += 4;
            }
        }
    }

    private void shadeCliffShadow(int sx, int sy, int drawW, int drawH, boolean nCliff, boolean wCliff,
                                  boolean shade) {
        int x0 = Math.max(0, sx);
        int y0 = Math.max(0, sy);
        int x1 = Math.min(VIEW_WIDTH, sx + drawW);
        int y1 = Math.min(SCREEN_HEIGHT, sy + drawH);
        if (x0 >= x1 || y0 >= y1) return;
        int shH = Math.max(2, drawH / 6);
        int shW = Math.max(2, drawW / 6);
        int baseAddr = screen.base;
        int stride = screen.width;
        for (int y = y0; y < y1; y++) {
            int dy = y - sy;
            int rowOffset = baseAddr + (y * stride + x0) * 4;
            for (int x = x0; x < x1; x++) {
                int dx = x - sx;
                int keep = 100;
                if (nCliff && dy < shH) {
                    keep = (keep * (100 - (40 * (shH - dy)) / shH)) / 100;
                }
                if (wCliff && dx < shW) {
                    keep = (keep * (100 - (40 * (shW - dx)) / shW)) / 100;
                }
                if (keep < 100) {
                    int col = Memory.get32(rowOffset);
                    int r = ((col & 0xFF) * keep) / 100;
                    int g = (((col >> 8) & 0xFF) * keep) / 100;
                    int b = (((col >> 16) & 0xFF) * keep) / 100;
                    int out = Palette.BLACK | (b << 16) | (g << 8) | r;
                    Memory.put32(rowOffset, shade ? shadeFogColor(out) : out);
                }
                rowOffset += 4;
            }
        }
    }

    private void blitOverlay(int sx, int sy, int drawW, int drawH, int[] pixels, boolean shade) {
        blitTile(sx, sy, drawW, drawH, shade ? foggedTile(pixels) : pixels);
    }

    private void blitTile(int sx, int sy, int drawW, int drawH, int[] pixels, boolean shade) {
        blitPixels(sx, sy, drawW, drawH, 64, 64, shade ? foggedTile(pixels) : pixels, 0);
    }

    private void blitTile(int sx, int sy, int drawW, int drawH, int[] pixels) {
        blitPixels(sx, sy, drawW, drawH, 64, 64, pixels, 0);
    }

    private void blitPixels(int sx, int sy, int drawW, int drawH, int srcW, int srcH,
                            int[] pixels, int factionColor) {
        if (drawW <= 0 || drawH <= 0 || pixels == null) return;
        int x0 = Math.max(0, sx);
        int y0 = Math.max(0, sy);
        int x1 = Math.min(VIEW_WIDTH, sx + drawW);
        int y1 = Math.min(SCREEN_HEIGHT, sy + drawH);
        if (x0 >= x1 || y0 >= y1) return;

        int stepX = (srcW << 16) / drawW;
        int stepY = (srcH << 16) / drawH;
        int baseAddr = screen.base;
        int stride = screen.width;
        for (int y = y0; y < y1; y++) {
            int srcRow = (((y - sy) * stepY) >>> 16) * srcW;
            int rowOffset = baseAddr + (y * stride + x0) * 4;
            for (int x = x0; x < x1; x++) {
                int col = pixels[srcRow + (((x - sx) * stepX) >>> 16)];
                if ((col & Palette.BLACK) != 0) {
                    if ((col & 0x00FFFFFF) == 0x000000FF && factionColor != 0) {
                        col = factionColor;
                    }
                    Memory.put32(rowOffset, col);
                }
                rowOffset += 4;
            }
        }
    }

    private static final int FOG_CACHE_MAX = 128;
    private static final int[][] fogKeys = new int[FOG_CACHE_MAX][];
    private static final int[][] fogVals = new int[FOG_CACHE_MAX][];
    private static int[] fogScratch = null;
    private static int fogCount = 0;
    private static int[] fogLastKey = null;
    private static int[] fogLastVal = null;

    private static int[] foggedTile(int[] src) {
        if (src == null) return null;
        if (src == fogLastKey) return fogLastVal;
        for (int i = 0; i < fogCount; i++) {
            if (fogKeys[i] == src) {
                int[] v = fogVals[i];
                fogLastKey = src;
                fogLastVal = v;
                if (i > 0) {
                    int[] k2 = fogKeys[i - 1];
                    int[] v2 = fogVals[i - 1];
                    fogKeys[i - 1] = src;
                    fogVals[i - 1] = v;
                    fogKeys[i] = k2;
                    fogVals[i] = v2;
                }
                return v;
            }
        }
        if (fogCount >= FOG_CACHE_MAX) {
            if (fogScratch == null || fogScratch.length < src.length) {
                fogScratch = new int[src.length];
            }
            for (int i = 0; i < src.length; i++) {
                int c = src[i];
                fogScratch[i] = (c >>> 24) == 0 ? 0 : shadeFogColor(c);
            }
            fogLastKey = null;
            fogLastVal = null;
            return fogScratch;
        }
        int[] out = new int[src.length];
        for (int i = 0; i < src.length; i++) {
            int c = src[i];
            out[i] = (c >>> 24) == 0 ? 0 : shadeFogColor(c);
        }
        fogKeys[fogCount] = src;
        fogVals[fogCount] = out;
        fogCount++;
        fogLastKey = src;
        fogLastVal = out;
        return out;
    }

    private static int div100(int v) {
        return (v * 5243) >> 19;
    }

    private static int shadeFogColor(int col) {
        int r = col & 0xFF;
        int g = (col >> 8) & 0xFF;
        int b = (col >> 16) & 0xFF;
        int lum = (r * 77 + g * 150 + b * 29) >> 8;
        int dr = div100(r * 35 + lum * 65);
        int dg = div100(g * 35 + lum * 65);
        int db = div100(b * 35 + lum * 65);
        int fr = div100(dr * 34);
        int fg = div100(dg * 36);
        int fb = div100(db * 43);
        return Palette.BLACK | (fb << 16) | (fg << 8) | fr;
    }

    private void blitSprite(int sx, int sy, int drawW, int drawH, int srcW, int srcH, int[] pixels, int factionColor) {
        blitPixels(sx, sy, drawW, drawH, srcW, srcH, pixels, factionColor);
    }

    private void blitSpriteConstructing(int sx, int sy, int drawW, int drawH, int srcW, int srcH, int[] pixels, int factionColor, double progress) {
        if (drawW <= 0 || drawH <= 0) return;
        int visibleH = (int) (drawH * progress);
        int cutoffY = drawH - visibleH;
        int stepX = (srcW << 16) / drawW;
        int stepY = (srcH << 16) / drawH;
        for (int y = 0; y < drawH; y++) {
            int py = sy + y;
            if (py < 0 || py >= SCREEN_HEIGHT) continue;
            boolean inBuiltPortion = (y >= cutoffY);
            int srcRow = ((y * stepY) >>> 16) * srcW;
            for (int x = 0; x < drawW; x++) {
                int px = sx + x;
                if (px < 0 || px >= VIEW_WIDTH) continue;
                int col = pixels[srcRow + ((x * stepX) >>> 16)];
                if ((col & Palette.BLACK) != 0) {
                    if ((col & 0x00FFFFFF) == 0x000000FF && factionColor != 0) {
                        col = factionColor;
                    }
                    if (inBuiltPortion) {
                        screen.setPixel(px, py, col);
                    } else if (((px + py) & 3) == 0) {
                        screen.setPixel(px, py, 0xFF506070);
                    }
                }
            }
        }
    }

    private int[] getBuildingSprite(BuildingType type, Faction faction) {
        return switch (type) {
            case WIND_TURBINE -> AssetData.BLDG_WIND_TRAP;
            case REFINERY -> AssetData.BLDG_REFINERY;
            case BARRACKS -> AssetData.BLDG_BARRACKS;
            case LIGHT_FACTORY -> AssetData.BLDG_LIGHT_FACTORY;
            case HEAVY_FACTORY -> AssetData.BLDG_HEAVY_FACTORY;
            case REPAIR_FACILITY -> (faction == Faction.ENEMY) ? AssetData.BLDG_REPAIR_SYNDICATE : AssetData.BLDG_REPAIR_VANGUARD;
            case RADAR -> AssetData.BLDG_RADAR;
            case TURRET -> AssetData.BLDG_TURRET;
            case CON_YARD -> AssetData.BLDG_CON_YARD;
        };
    }

    private int[] getUnitSprite(UnitType type, Faction faction) {
        return switch (type) {
            case COLLECTOR -> AssetData.UNIT_COLLECTOR;
            case SCOUT_TRIKE -> AssetData.UNIT_SCOUT_TRIKE;
            case COMBAT_TANK -> AssetData.UNIT_COMBAT_TANK;
            case SIEGE_TANK -> AssetData.UNIT_SIEGE_TANK;
            case TROOPER_RIFLE -> AssetData.UNIT_TROOPER_RIFLE;
            case TROOPER_MISSILE -> AssetData.UNIT_TROOPER_MISSILE;
            case MECHANIC -> (faction == Faction.ENEMY) ? AssetData.UNIT_MECHANIC_SYNDICATE : AssetData.UNIT_MECHANIC_VANGUARD;
            case MEDIC -> (faction == Faction.ENEMY) ? AssetData.UNIT_MEDIC_SYNDICATE : AssetData.UNIT_MEDIC_VANGUARD;
            case REAVER_BUGGY -> AssetData.UNIT_REAVER_BUGGY;
            case RAVAGER_TANK -> AssetData.UNIT_RAVAGER_TANK;
            case DREADNOUGHT -> AssetData.UNIT_DREADNOUGHT;
            case RAIDER -> AssetData.UNIT_RAIDER;
            case VANDAL -> AssetData.UNIT_VANDAL;
            case SHARD_SKITTERER -> AssetData.UNIT_SHARD_SKITTERER;
            case SCAVENGER_DRONE -> AssetData.UNIT_SCAVENGER_DRONE;
            case PRECURSOR_WALKER -> AssetData.UNIT_PRECURSOR_WALKER;
        };
    }

    private void renderRadarOverlay(Building b, int sx, int sy, int bw, int bh, double time, double zoom) {
        int cx = sx + bw / 2;
        int cy = sy + bh / 2;
        double dishAngle = time * 2.5;
        int dx = (int)(Math.cos(dishAngle) * (23.0 * zoom));
        int dy = (int)(Math.sin(dishAngle) * (11.0 * zoom));
        screen.drawLine(cx - dx, cy - dy, cx + dx, cy + dy, Palette.YELLOW_PALE);
        screen.drawLine(cx, cy, cx - dy, cy + dx, Palette.GOLD);
        screen.setPixel(cx, cy, Palette.WHITE);
    }

    private void renderTurretOverlay(Building b, int sx, int sy, int bw, int bh, double time, double zoom) {
        int cx = sx + bw / 2;
        int cy = sy + bh / 2;
        int barrelLen = Math.max(6, (int) (36 * zoom));
        double a = b.turretAngle;
        double perpA = a + Math.PI / 2.0;
        int px = (int)(Math.cos(perpA) * (7.0 * zoom));
        int py = (int)(Math.sin(perpA) * (7.0 * zoom));

        int b1x = cx + px + (int)(Math.cos(a) * barrelLen);
        int b1y = cy + py + (int)(Math.sin(a) * barrelLen);
        int b2x = cx - px + (int)(Math.cos(a) * barrelLen);
        int b2y = cy - py + (int)(Math.sin(a) * barrelLen);

        screen.drawLine(cx + px, cy + py, b1x, b1y, 0xFFD8D8E0);
        screen.drawLine(cx - px, cy - py, b2x, b2y, 0xFFD8D8E0);
        screen.setPixel(b1x, b1y, Palette.GRAY_DARK);
        screen.setPixel(b2x, b2y, Palette.GRAY_DARK);
    }

    private void renderConYardOverlay(Building b, int sx, int sy, int bw, int bh, double time, double zoom) {
        int cx = sx + bw / 2;
        int cy = sy + bh / 2;
        double craneAngle = Math.sin(time * 0.8) * 0.8 + 0.8;
        int armLen = Math.max(12, (int) (59 * zoom));
        int tipX = cx + (int)(Math.cos(craneAngle) * armLen);
        int tipY = cy + (int)(Math.sin(craneAngle) * armLen);
        screen.drawLine(cx, cy, tipX, tipY, Palette.GOLD_DARK);
        screen.drawLine(tipX, tipY, tipX, tipY + Math.max(4, (int) (16 * zoom)), 0xFFD0D0D0);
    }

    private void renderTankTurretOverlay(Unit u, int sx, int sy, double zoom) {
        boolean twin = u.type.turret == UnitType.Turret.TWIN;
        int barrelLen = Math.max(6, (int) ((twin ? 37 : 30) * zoom));
        double a = u.turretAngle;
        if (twin) {
            double perp = a + Math.PI / 2.0;
            int px = (int)(Math.cos(perp) * (5.0 * zoom));
            int py = (int)(Math.sin(perp) * (5.0 * zoom));
            screen.drawLine(sx + px, sy + py, sx + px + (int)(Math.cos(a) * barrelLen), sy + py + (int)(Math.sin(a) * barrelLen), Palette.GRAY_LIGHT);
            screen.drawLine(sx - px, sy - py, sx - px + (int)(Math.cos(a) * barrelLen), sy - py + (int)(Math.sin(a) * barrelLen), Palette.GRAY_LIGHT);
        } else {
            screen.drawLine(sx, sy, sx + (int)(Math.cos(a) * barrelLen), sy + (int)(Math.sin(a) * barrelLen), Palette.GRAY_LIGHT);
        }
    }

    private void renderBuilding(Building b, int sx, int sy, int bw, int bh, double time, WarTech game, Camera camera) {
        drawShadowRect(screen, sx + Math.max(2, (int) (5 * camera.zoom)), sy + Math.max(2, (int) (6 * camera.zoom)), bw, bh);

        screen.fillRect(sx + 1, sy + 1, bw - 2, bh - 2, 0xFF32302E);
        screen.drawLine(sx + 1, sy + 1, sx + bw - 2, sy + 1, 0xFF585450);
        screen.drawLine(sx + 1, sy + 1, sx + 1, sy + bh - 2, 0xFF585450);
        screen.drawLine(sx + bw - 2, sy + 1, sx + bw - 2, sy + bh - 2, 0xFF181614);
        screen.drawLine(sx + 1, sy + bh - 2, sx + bw - 2, sy + bh - 2, 0xFF181614);

        for (int x = sx + 4; x < sx + bw - 4; x++) {
            int col = ((x - sx) % 6 < 3) ? Palette.GOLD_DARK : 0xFF151515;
            screen.setPixel(x, sy + bh - 3, col);
        }

        int[] bSprite = getBuildingSprite(b.type, b.faction);
        int srcW = b.type.widthTiles * GameMap.TILE_SIZE;
        int srcH = b.type.heightTiles * GameMap.TILE_SIZE;
        if (b.constructing) {
            for (int gx = sx + 6; gx < sx + bw - 6; gx += Math.max(6, (int) (12 * camera.zoom))) {
                screen.drawLine(gx, sy + 4, gx, sy + bh - 4, 0xFF354555);
            }
            if (bSprite != null) {
                blitSpriteConstructing(sx, sy, bw, bh, srcW, srcH, bSprite, b.faction.primaryColor, b.constructionProgress);
            }

            int barW = Math.max(24, bw - 12);
            int barH = Math.max(4, (int) (6 * camera.zoom));
            int barX = sx + (bw - barW) / 2;
            int barY = sy + bh / 2 - barH / 2;
            screen.fillRect(barX - 1, barY - 1, barW + 2, barH + 2, Palette.BLACK);
            screen.fillRect(barX, barY, barW, barH, 0xFF181C18);
            int fillW = (int) (b.constructionProgress * barW);
            int barCol = (game != null && game.playerCredits < WarTech.BUILD_SPEND_PER_TICK) ? Palette.GOLD_DARK : Palette.GREEN;
            screen.fillRect(barX, barY, fillW, barH, barCol);

            int pct = (int) (b.constructionProgress * 100);
            boolean hold = game != null && game.playerCredits < WarTech.BUILD_SPEND_PER_TICK;
            int pctW = hold ? BitmapFont.getTextWidth("HOLD") : BitmapFont.intWidth(pct, 1) + 6;
            int pctX = barX + (barW - pctW) / 2;
            if (hold) {
                BitmapFont.drawText(screen, "HOLD", pctX, barY - 8, Palette.WHITE, 1);
            } else {
                BitmapFont.drawLine(screen, null, pct, "%", pctX, barY - 8, Palette.WHITE);
            }
        } else {
            if (bSprite != null) {
                blitSprite(sx, sy, bw, bh, srcW, srcH, bSprite, b.faction.primaryColor);
            }

            switch (b.type) {
                case RADAR -> renderRadarOverlay(b, sx, sy, bw, bh, time, camera.zoom);
                case TURRET -> renderTurretOverlay(b, sx, sy, bw, bh, time, camera.zoom);
                case CON_YARD -> renderConYardOverlay(b, sx, sy, bw, bh, time, camera.zoom);
                case null, default -> { }
            }
        }

        if (b.hp < b.maxHp / 2) {
            int smokeOffset = (int) (time * 44.0) % 48;
            int smokeW = Math.max(4, (int) (16 * camera.zoom));
            screen.fillRect(sx + bw / 3, sy + bh / 3 - smokeOffset, smokeW, smokeW, 0xFF404040);
            screen.fillRect(sx + bw / 2, sy + bh / 2 - (smokeOffset + 16) % 48, (smokeW * 4) / 3, (smokeW * 4) / 3, 0xFF2A2A2A);
            if ((int)(time * 10.0) % 2 == 0) {
                screen.fillRect(sx + bw / 3 + 2, sy + bh / 3 + 4, Math.max(2, (int) (11 * camera.zoom)), Math.max(2, (int) (11 * camera.zoom)), 0xFF0050FF); // Fire ember
            }
        }

        if (game != null && b == game.selectedBuilding) {
            int selCol = (b.faction == Faction.PLAYER) ? Palette.GREEN_PURE : Palette.RED;
            int bl = Math.max(6, (int) (21 * camera.zoom));
            screen.drawLine(sx - 2, sy - 2, sx - 2 + bl, sy - 2, selCol);
            screen.drawLine(sx - 2, sy - 2, sx - 2, sy - 2 + bl, selCol);

            screen.drawLine(sx + bw + 1, sy - 2, sx + bw + 1 - bl, sy - 2, selCol);
            screen.drawLine(sx + bw + 1, sy - 2, sx + bw + 1, sy - 2 + bl, selCol);

            screen.drawLine(sx - 2, sy + bh + 1, sx - 2 + bl, sy + bh + 1, selCol);
            screen.drawLine(sx - 2, sy + bh + 1, sx - 2, sy + bh + 1 - bl, selCol);

            screen.drawLine(sx + bw + 1, sy + bh + 1, sx + bw + 1 - bl, sy + bh + 1, selCol);
            screen.drawLine(sx + bw + 1, sy + bh + 1, sx + bw + 1, sy + bh + 1 - bl, selCol);

            String bName = b.type.displayNameUpper;
            int tagW = bName.length() * 6;
            BitmapFont.drawTextWithShadow(screen, bName, sx + (bw - tagW) / 2, sy - (camera.zoom < 0.35 ? 18 : 26), selCol, 1);
        }

        if ((game != null && b == game.selectedBuilding) || b.hp < b.maxHp) {
            int barW = Math.max(16, bw - 8);
            int hpW = (int) ((double) b.hp / b.maxHp * barW);
            screen.fillRect(sx + 4, sy - 7, barW, 5, Palette.BLACK);
            int hpCol = (b.hp > b.maxHp / 2) ? Palette.GREEN : (b.hp > b.maxHp / 4 ? Palette.BLUE_SKY : Palette.RED);
            screen.fillRect(sx + 4, sy - 7, hpW, 5, hpCol);
            screen.drawLine(sx + 4, sy - 7, sx + 4 + barW, sy - 7, 0xFF404040);
        }
    }

    private void renderUnit(Unit u, int sx, int sy, double time, Camera camera, WarTech game) {
        double zoom = camera.zoom;
        int size = Math.max(2, (int) (u.type.radius * zoom));

        drawShadowEllipse(screen, sx + 3, sy + (size * 2) / 3, size, Math.max(1, (size * 2) / 3));

        int[] uSprite = getUnitSprite(u.type, u.faction);
        int uDrawSize = Math.max(16, (int) (GameMap.TILE_SIZE * zoom));
        int half = uDrawSize / 2;
        if (uSprite != null) {
            blitSprite(sx - half, sy - half, uDrawSize, uDrawSize, GameMap.TILE_SIZE, GameMap.TILE_SIZE, uSprite, u.faction.primaryColor);
            if (u.type.turret != UnitType.Turret.NONE) {
                renderTankTurretOverlay(u, sx, sy, zoom);
            }
        } else {
            screen.fillRect(sx - size, sy - size, size * 2, size * 2, u.faction.primaryColor);
        }

        boolean isInspected = (game != null && game.selectedUnit == u);
        boolean isSelected = u.selected || isInspected;

        if (isSelected) {
            int bs = size + Math.max(4, (int) (14 * zoom));
            int selCol = (u.faction == Faction.PLAYER) ? Palette.GREEN_PURE : (u.faction == Faction.ENEMY ? Palette.RED : Palette.GOLD_LIGHT);
            int bl = Math.max(4, (int) (14 * zoom));
            
            screen.drawLine(sx - bs, sy - bs, sx - bs + bl, sy - bs, selCol);
            screen.drawLine(sx - bs, sy - bs, sx - bs, sy - bs + bl, selCol);
            
            screen.drawLine(sx + bs, sy - bs, sx + bs - bl, sy - bs, selCol);
            screen.drawLine(sx + bs, sy - bs, sx + bs, sy - bs + bl, selCol);
            
            screen.drawLine(sx - bs, sy + bs, sx - bs + bl, sy + bs, selCol);
            screen.drawLine(sx - bs, sy + bs - bl, sx - bs, sy + bs, selCol);
            
            screen.drawLine(sx + bs, sy + bs, sx + bs - bl, sy + bs, selCol);
            screen.drawLine(sx + bs, sy + bs - bl, sx + bs, sy + bs, selCol);

            if (u.moving && u.faction == Faction.PLAYER && camera != null) {
                int tx = camera.worldToScreenX(u.targetX);
                int ty = camera.worldToScreenY(u.targetY);
                drawDottedLine(sx, sy, tx, ty, Palette.GREEN_BRIGHT);
                screen.fillRect(tx - 1, ty - 1, 3, 3, Palette.GREEN_PURE);
            }
            if (u.attackTargetUnit != null && !u.attackTargetUnit.isDestroyed() && camera != null) {
                int tx = camera.worldToScreenX(u.attackTargetUnit.x);
                int ty = camera.worldToScreenY(u.attackTargetUnit.y);
                drawDottedLine(sx, sy, tx, ty, Palette.RED);
            }

            String uName = u.type.displayNameUpper;
            int tagW = uName.length() * 6;
            int tagY = sy - size - (zoom < 0.35 ? 14 : 20);
            BitmapFont.drawTextWithShadow(screen, uName, sx - tagW / 2, tagY, selCol, 1);
        }

        if (isSelected || u.hp < u.maxHp) {
            int barW = Math.max(16, size * 2 + 4);
            int hpW = (int) ((double) u.hp / u.maxHp * barW);
            screen.fillRect(sx - barW / 2 - 1, sy - size - 8, barW + 2, 4, Palette.BLACK);
            int hpCol = (u.hp > u.maxHp / 2) ? Palette.GREEN : (u.hp > u.maxHp / 4 ? Palette.BLUE_SKY : Palette.RED);
            screen.fillRect(sx - barW / 2, sy - size - 7, hpW, 2, hpCol);
        }

        if (u.type == UnitType.MECHANIC && u.isRepairing && camera != null) {
            int tx = -1;
            int ty = -1;
            if (u.repairTargetUnit != null && !u.repairTargetUnit.isDestroyed()) {
                tx = camera.worldToScreenX(u.repairTargetUnit.x);
                ty = camera.worldToScreenY(u.repairTargetUnit.y);
            } else if (u.repairTargetBuilding != null && !u.repairTargetBuilding.isDestroyed()) {
                tx = camera.worldToScreenX(u.repairTargetBuilding.getCenterX());
                ty = camera.worldToScreenY(u.repairTargetBuilding.getCenterY());
            }
            if (tx >= 0 && tx < VIEW_WIDTH && ty >= 0 && ty < SCREEN_HEIGHT) {
                int arcCol = (((int) (time * 24.0) & 1) == 0) ? Palette.GOLD_LIGHT : Palette.WHITE;
                screen.drawLine(sx, sy, tx, ty, arcCol);
            }
        }
    }

    private void drawDottedLine(int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int step = 0;
        while (true) {
            if (step % 6 < 3) {
                if (x0 >= 0 && x0 < VIEW_WIDTH && y0 >= 0 && y0 < SCREEN_HEIGHT) {
                    screen.setPixel(x0, y0, color);
                }
            }
            step++;
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    private void drawPixelSafe(int px, int py, int col) {
        if (px >= 0 && px < VIEW_WIDTH && py >= 0 && py < SCREEN_HEIGHT) {
            screen.setPixel(px, py, col);
        }
    }

    private void drawPuffSafe(int cx, int cy, int r, int col) {
        for (int dy = -r; dy <= r; dy++) {
            int py = cy + dy;
            if (py < 0 || py >= SCREEN_HEIGHT) continue;
            for (int dx = -r; dx <= r; dx++) {
                int px = cx + dx;
                if (px < 0 || px >= VIEW_WIDTH) continue;
                if (dx * dx + dy * dy <= r * r) {
                    screen.setPixel(px, py, col);
                }
            }
        }
    }

    private void renderEffect(Effect fx, int sx, int sy, double zoom) {
        double p = Math.clamp(fx.timer / fx.maxDuration, 0.0, 1.0);
        double invP = 1.0 - p;
        double effRadius = Math.max(2.0, fx.radius * zoom);

        switch (fx.type) {
            case Effect.TYPE_HARVEST_DUST -> {
            double curR = effRadius * p;
            int coreR = (int) (effRadius * 0.35 * invP);
            if (coreR > 0) {
                drawPuffSafe(sx, sy, coreR, 0xFF457090); 
            }
            for (int k = 0; k < 8; k++) {
                double angle = k * (Math.PI / 4.0) + (k * 0.5);
                int px = sx + (int) (Math.cos(angle) * curR);
                int py = sy + (int) (Math.sin(angle) * curR * 0.7);
                int col = (k % 2 == 0) ? 0xFF00E0FF : 0xFF6595B5; 
                drawPixelSafe(px, py, col);
                drawPixelSafe(px + 1, py, col);
                if (invP > 0.4) {
                    drawPixelSafe(px, py + 1, col);
                }
            }
            }
            case Effect.TYPE_MUZZLE -> {
            int len = Math.max(1, (int) (effRadius * invP));
            if (sx - len >= 0 && sx + len < VIEW_WIDTH) {
                screen.drawLine(Math.max(0, sx - len), sy, Math.min(VIEW_WIDTH - 1, sx + len), sy, 0xFF00D0FF);
            }
            if (sy - len >= 0 && sy + len < SCREEN_HEIGHT) {
                screen.drawLine(sx, Math.max(0, sy - len), sx, Math.min(SCREEN_HEIGHT - 1, sy + len), 0xFF00D0FF);
            }
            int diag = len * 7 / 10;
            if (diag > 0) {
                drawPixelSafe(sx - diag, sy - diag, 0xFF0090FF);
                drawPixelSafe(sx + diag, sy - diag, 0xFF0090FF);
                drawPixelSafe(sx - diag, sy + diag, 0xFF0090FF);
                drawPixelSafe(sx + diag, sy + diag, 0xFF0090FF);
            }
            drawPixelSafe(sx, sy, Palette.WHITE);
            drawPixelSafe(sx + 1, sy, Palette.WHITE);
            drawPixelSafe(sx, sy + 1, Palette.WHITE);
            drawPixelSafe(sx - 1, sy, Palette.WHITE);
            drawPixelSafe(sx, sy - 1, Palette.WHITE);
            }
            case Effect.TYPE_EXPLOSION -> {
            int curR = (int) (effRadius * (0.2 + 0.8 * p));
            int coreR = (int) (effRadius * 0.5 * invP);

            if (coreR > 0) {
                drawPuffSafe(sx, sy, coreR, (p < 0.3) ? Palette.WHITE : 0xFF00C0FF);
            }

            if (curR > 1) {
                int ringCol = (p < 0.5) ? 0xFF0060FF : 0xFF0020D0;
                for (int k = 0; k < 12; k++) {
                    double a = k * (Math.PI / 6.0);
                    int rx = sx + (int) (Math.cos(a) * curR);
                    int ry = sy + (int) (Math.sin(a) * curR);
                    drawPixelSafe(rx, ry, ringCol);
                }
            }

            for (int s = 0; s < 4; s++) {
                double a = s * (Math.PI / 2.0) + 0.4;
                double sd = curR * 0.75;
                drawPixelSafe(sx + (int) (Math.cos(a) * sd), sy + (int) (Math.sin(a) * sd), 0xFF282828);
            }
            }
            case Effect.TYPE_HEAL -> {
            int curY = sy - (int) (p * 18.0 * zoom);
            int arm = Math.max(2, (int) (4.0 * zoom));
            int col = (p < 0.7) ? Palette.GREEN : 0xFF80FFB0;
            if (curY >= 0 && curY < SCREEN_HEIGHT) {
                screen.drawLine(Math.max(0, sx - arm), curY, Math.min(VIEW_WIDTH - 1, sx + arm), curY, col);
            }
            if (sx >= 0 && sx < VIEW_WIDTH) {
                screen.drawLine(sx, Math.max(0, curY - arm), sx, Math.min(SCREEN_HEIGHT - 1, curY + arm), col);
            }
            drawPixelSafe(sx, curY, Palette.WHITE);
            }
            case Effect.TYPE_ORDER_MOVE, Effect.TYPE_ORDER_ATTACK -> {
            int col = (fx.type == Effect.TYPE_ORDER_ATTACK) ? Palette.RED : Palette.GREEN_PURE;
            int r = Math.max(2, (int) (effRadius * (0.35 + 0.65 * invP)));
            for (int k = 0; k < 16; k++) {
                double a = k * (Math.PI / 8.0);
                drawPixelSafe(sx + (int) (Math.cos(a) * r), sy + (int) (Math.sin(a) * r), col);
            }
            for (int o = r + 2; o <= r + 5; o++) {
                drawPixelSafe(sx - o, sy, col);
                drawPixelSafe(sx + o, sy, col);
                drawPixelSafe(sx, sy - o, col);
                drawPixelSafe(sx, sy + o, col);
            }
            if (invP > 0.5) {
                drawPixelSafe(sx, sy, Palette.WHITE);
            }
            }
            default -> {
            int len = Math.max(1, (int) (effRadius * invP));
            for (int s = 0; s < 4; s++) {
                double a = s * (Math.PI / 2.0) + p * 2.0;
                int px = sx + (int) (Math.cos(a) * len);
                int py = sy + (int) (Math.sin(a) * len);
                drawPixelSafe(px, py, 0xFF00FFFF);
            }
            drawPixelSafe(sx, sy, Palette.WHITE);
            }
        }
    }
}
