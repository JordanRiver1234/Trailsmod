package net.JordanRiver.KisekiLegend.items;

import net.JordanRiver.KisekiLegend.fishing.FishData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

public class FishItem extends Item {
    private final String fishType;

    public FishItem(String fishType, Properties properties) {
        super(properties);
        this.fishType = fishType;
    }
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack fishStack = context.getItemInHand();

        // Check if this fish can be used as bait
        FishData fishData = net.JordanRiver.KisekiLegend.fishing.FishRegistry.getFishData(fishType);
        if (fishData == null || !fishData.canBeBait()) {
            return InteractionResult.PASS;
        }

        // Check player's hands for fishing rod
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        ItemStack rodStack = null;
        if (mainHand.getItem() instanceof KisekiFishingRodItem && mainHand != fishStack) {
            rodStack = mainHand;
        } else if (offHand.getItem() instanceof KisekiFishingRodItem && offHand != fishStack) {
            rodStack = offHand;
        }

        if (rodStack != null) {
            return KisekiFishingRodItem.tryAttachBait(rodStack, fishStack, player) ?
                    InteractionResult.SUCCESS : InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        String description = getFishDescription(fishType);
        if (description != null) {
            tooltipComponents.add(Component.literal(description));
        }
        // Check if this fish can be used as bait
        if (net.JordanRiver.KisekiLegend.fishing.FishRegistry.getFishData(fishType) != null) {
            if (net.JordanRiver.KisekiLegend.fishing.FishRegistry.getFishData(fishType).canBeBait()) {
                tooltipComponents.add(Component.literal("Can be used as bait"));
            }
        }

    }
    private String getFishDescription(String fishType) {
        return switch (fishType) {
            case "crab" -> "Crab that likes rock shadows. Used as bait, will sometimes catch really big fish.";
            case "dace" -> "Small fish very tolerant to changes in its environment. Found throughout Liberl.";
            case "gold_angelfish" -> "Small fish that inhabits the Azelia Bay. Has a bright, blindingly gold body.";
            case "liberl_carp" -> "River fish characterized by its tiny beard. Found in just about all freshwater areas.";
            case "kasago" -> "A fish that inhabits small, complex areas like rocky waterfalls.";
            case "valleria_bass" -> "Bad-tasting fish that inhabits the lake bed. Eats everything and anything.";
            case "rockeater" -> "Medium-sized fish that hide in the shadow of rocky overhangs. Extremely cautious.";
            case "great_blackfish" -> "Amongst fishers, this fish is considered the standard for surf fishing.";
            case "carp" -> "A long-lived fish, with great vigor. Lives an average of 15-20 years.";
            case "octopus" -> "A mollusk that inhabits portions of the Azelia Bay. Has a monstrous appearance.";
            case "rainbow_trout" -> "Medium-sized fish with rainbow-streaked scales. Loves sepith.";
            case "trout" -> "A landlocked offshoot of Salmon. Known for its right steel blue flesh.";
            case "eel" -> "A fish valued in the East for imbuing vim and vigor into the consumer.";
            case "salmon" -> "The symbol of upstream migratory fish. Born in Valleria Lake, they migrate to the sea and return to the Roubine River to lay their eggs.";
            case "claudine" -> "A large fish with pitch-black scales. Famous for its sequential hermaphroditism.";
            case "snakehead" -> "Ferocious fish that prefers stagnant water. Has an undulating gait.";
            case "pearlglass" -> "Large freshwater fish with a body that shines like silver. Calm and quiet.";
            case "garvelze" -> "Large fish that likes muddy water bottoms. Also called the Lakebottom Brawler.";
            case "sea_bass" -> "A fish that has multiple names as it ages. Goes from Seib to a Sea Bass to a Sea Vic.";
            case "gigangora" -> "A giant fish that inhabits the bottom of Azelia Bay. Grotesque in appearance.";
            case "mahimahi" -> "A monstrously large fish over 1.5 arge in length.";
            case "tiger_rockfish" -> "A variety of rockfish. Has a beautiful orange abdomen.";
            case "granakor" -> "A gigantic crab, over 2 arge in length with both claws spread.";
            case "blue_marlin" -> "Also called the Blue Noble. A fish whose sea-colored scales glimmer like jewels.";
            case "yamany" -> "Small river fish that loves clear streams. Known for its spotted pattern.";
            case "dynatrad" -> "A fish that has broken many challengers before. The fabled King of Valleria Lake.";
            // Add all other descriptions...
            default -> null;
        };
    }
    public String getFishType() {
        return fishType;
    }
}