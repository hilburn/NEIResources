package neiresources.nei;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;
import neiresources.reference.Resources;
import neiresources.registry.DungeonRegistry;
import neiresources.registry.DungeonRegistryEntry;
import neiresources.utils.Font;
import neiresources.utils.RenderHelper;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class NEIDungeonHandler extends TemplateRecipeHandler
{
    private static final int X_FIRST_ITEM = -2;
    private static final int Y_FIRST_ITEM = 48;
    private static final int ITEMS_PER_COLUMN = 4;
    private static final int ITEMS_PER_ROW = 3;
    private static final int ITEMS_PER_PAGE = ITEMS_PER_COLUMN * ITEMS_PER_ROW;
    private static final int SPACING_X = 176 / ITEMS_PER_ROW;
    private static final int SPACING_Y = 80 / ITEMS_PER_COLUMN;
    private static final int CYCLE_TIME = 30;
    private static int lidStart = -1;
    private static int lastRecipe = -1;
    private static boolean done;

    @Override
    public String getGuiTexture()
    {
        return Resources.Gui.DUNGEON_NEI.toString();
    }

    @Override
    public String getRecipeName()
    {
        return "Dungeon Chest";
    }

    @Override
    public int recipiesPerPage()
    {
        return 1;
    }

    @Override
    public void loadCraftingRecipes(ItemStack result)
    {
        for (DungeonRegistryEntry entry : DungeonRegistry.getInstance().getDungeons(result))
            arecipes.add(new CachedDungeonChest(entry));
    }

    @Override
    public void drawBackground(int recipe)
    {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GuiDraw.changeTexture(this.getGuiTexture());
        GuiDraw.drawTexturedModalRect(0, 0, 5, 11, 166, 130);

        RenderHelper.renderChest(15, 20, -40, 20, getLidAngle(recipe));
    }

    private float getLidAngle(int recipe)
    {
        if (recipe != lastRecipe)
        {
            done = false;
            lastRecipe = -1;
            lidStart = -1;
        }

        if (lidStart == -1) lidStart = cycleticks;

        float angle = (cycleticks - lidStart) % 80;
        if (angle > 50 || done)
        {
            done = true;
            angle = 50;
        }

        return angle;
    }

    @Override
    public void drawExtras(int recipe)
    {
        CachedDungeonChest cachedChest = (CachedDungeonChest)arecipes.get(recipe);

        Font font = new Font(false);
        font.print(cachedChest.chest.getName(), 60, 10);
        font.print(cachedChest.chest.getNumStacks(), 60, 25);

        int x = X_FIRST_ITEM + 18;
        int y = Y_FIRST_ITEM + (10-ITEMS_PER_COLUMN);
        for (int i = ITEMS_PER_PAGE * cachedChest.set; i < ITEMS_PER_PAGE * cachedChest.set + ITEMS_PER_PAGE; i++)
        {
            if (i >= cachedChest.chest.getContents().length) break;
            double chance = cachedChest.chest.getChance(cachedChest.chest.getContents()[i]) * 100;
            String format = chance < 100 ? "%2.1f" : "%2.0f";
            String toPrint = String.format(format, chance).replace(',', '.') + "%";
            font.print(toPrint, x, y);
            y += SPACING_Y;
            if (y >= Y_FIRST_ITEM + SPACING_Y * ITEMS_PER_COLUMN)
            {
                y = Y_FIRST_ITEM + (10-ITEMS_PER_COLUMN);
                x += SPACING_X;
            }
        }

        cachedChest.cycleOutputs(cycleticks, recipe);
    }

    public class CachedDungeonChest extends TemplateRecipeHandler.CachedRecipe
    {

        public DungeonRegistryEntry chest;
        public int set, sets;
        private long cycleAt;

        public CachedDungeonChest(DungeonRegistryEntry chest)
        {
            this.chest = chest;
            set = 0;
            cycleAt = -1;
            sets = (chest.getContents().length / ITEMS_PER_PAGE) +1;
        }

        @Override
        public PositionedStack getResult()
        {
            return new PositionedStack(this.chest.getContents()[set*ITEMS_PER_PAGE].theItemId, X_FIRST_ITEM, Y_FIRST_ITEM);
        }

        @Override
        public List<PositionedStack> getOtherStacks()
        {
            List<PositionedStack> list = new ArrayList<PositionedStack>();
            int x = X_FIRST_ITEM;
            int y = Y_FIRST_ITEM;
            for (int i = ITEMS_PER_PAGE * set; i < ITEMS_PER_PAGE * set + ITEMS_PER_PAGE; i++)
            {
                if (i >= this.chest.getContents().length) break;
                list.add(new PositionedStack(this.chest.getContents()[i].theItemId, x, y));
                y += SPACING_Y;
                if (y >= Y_FIRST_ITEM + SPACING_Y * ITEMS_PER_COLUMN)
                {
                    y = Y_FIRST_ITEM;
                    x += SPACING_X;
                }
            }
            list.remove(0);
            return list;
        }

        public void cycleOutputs(long tick, int recipe)
        {
            if (cycleAt == -1 || recipe != lastRecipe)
            {
                lastRecipe = recipe;
                cycleAt = tick + CYCLE_TIME;
                return;
            }

            if (tick >= cycleAt)
            {
                if (++set >= sets) set = 0;
                cycleAt += CYCLE_TIME;
            }
        }
    }
}