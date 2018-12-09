package p455w0rd.ae2wtlib.init;

import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;

import appeng.tile.networking.TileController;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.player.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import p455w0rd.ae2wtlib.AE2WTLib;
import p455w0rd.ae2wtlib.client.render.BaubleRenderDispatcher;
import p455w0rd.ae2wtlib.init.LibIntegration.Mods;
import p455w0rd.ae2wtlib.sync.WTPacket;
import p455w0rd.ae2wtlib.sync.packets.PacketConfigSync;
import p455w0rd.ae2wtlib.sync.packets.PacketSyncInfinityEnergyInv;
import p455w0rd.ae2wtlib.util.WTUtils;
import p455w0rdslib.capabilities.CapabilityChunkLoader;
import p455w0rdslib.capabilities.CapabilityChunkLoader.ProviderTE;

/**
 * @author p455w0rd
 *
 */
@EventBusSubscriber(modid = LibGlobals.MODID)
public class LibEvents {

	@SubscribeEvent
	public static void onItemRegistryReady(RegistryEvent.Register<Item> event) {
		event.getRegistry().registerAll(LibItems.getArray());
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public static void onModelBake(ModelBakeEvent event) {
		LibItems.initModels(event);
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public static void onModelRegister(ModelRegistryEvent event) {
		LibItems.registerTEISRs(event);
		//ModItems.WCT.setTileEntityItemStackRenderer(new WTItemRenderer().setModel(ModItems.WCT.getWrappedModel()));
		//ModItems.CREATIVE_WCT.setTileEntityItemStackRenderer(new WTItemRenderer().setModel(ModItems.WCT.getWrappedModel()));
		//ModItems.WFT.setTileEntityItemStackRenderer(new WFTItemRenderer());
		//ModItems.CREATIVE_WFT.setTileEntityItemStackRenderer(new WFTItemRenderer());
	}

	@SubscribeEvent
	public static void attachCapabilities(AttachCapabilitiesEvent<TileEntity> event) {
		if (event.getObject() instanceof TileController && LibConfig.WT_ENABLE_CONTROLLER_CHUNKLOADER) {
			TileController controller = (TileController) event.getObject();
			event.addCapability(new ResourceLocation(LibGlobals.MODID, "chunkloader"), new ProviderTE(controller));
		}
	}

	@SubscribeEvent
	public static void onPlace(BlockEvent.PlaceEvent e) {
		World world = e.getWorld();
		BlockPos pos = e.getPos();
		if (world != null && pos != null && world.getTileEntity(pos) != null && !world.isRemote && LibConfig.WT_ENABLE_CONTROLLER_CHUNKLOADER) {
			if (world.getTileEntity(pos) instanceof TileController) {
				TileEntity tile = world.getTileEntity(pos);
				if (tile.hasCapability(CapabilityChunkLoader.CAPABILITY_CHUNKLOADER_TE, null)) {
					tile.getCapability(CapabilityChunkLoader.CAPABILITY_CHUNKLOADER_TE, null).attachChunkLoader(AE2WTLib.INSTANCE);
				}
			}
		}
	}

	@SubscribeEvent
	public static void onBreak(BlockEvent.BreakEvent e) {
		World world = e.getWorld();
		BlockPos pos = e.getPos();
		if (world != null && pos != null && world.getTileEntity(pos) != null && !world.isRemote && LibConfig.WT_ENABLE_CONTROLLER_CHUNKLOADER) {
			if (world.getTileEntity(pos) instanceof TileController) {
				TileEntity tile = world.getTileEntity(pos);
				if (tile.hasCapability(CapabilityChunkLoader.CAPABILITY_CHUNKLOADER_TE, null)) {
					tile.getCapability(CapabilityChunkLoader.CAPABILITY_CHUNKLOADER_TE, null).detachChunkLoader(AE2WTLib.INSTANCE);
				}
			}
		}
	}

	@SubscribeEvent
	public static void onMobDrop(LivingDropsEvent event) {
		ItemStack stack = new ItemStack(LibItems.BOOSTER_CARD);
		EntityItem drop = new EntityItem(event.getEntityLiving().getEntityWorld(), event.getEntity().posX, event.getEntity().posY, event.getEntity().posZ, stack);
		if (event.getEntity() instanceof EntityDragon && LibConfig.WT_BOOSTER_ENABLED && LibConfig.WT_DRAGON_DROPS_BOOSTER) {
			event.getDrops().add(drop);
		}

		if (event.getEntity() instanceof EntityWither && LibConfig.WT_BOOSTER_ENABLED && LibConfig.WT_WITHER_DROPS_BOOSTER) {
			Random rand = event.getEntityLiving().getEntityWorld().rand;
			int n = rand.nextInt(100);
			if (n <= LibConfig.WT_BOOSTER_DROP_CHANCE) {
				event.getDrops().add(drop);
			}
		}

		if (event.getEntity() instanceof EntityEnderman && LibConfig.WT_BOOSTER_ENABLED && LibConfig.WT_ENDERMAN_DROP_BOOSTERS) {
			Random rand = event.getEntityLiving().getEntityWorld().rand;
			int n = rand.nextInt(100);
			if (n <= LibConfig.WT_ENDERMAN_BOOSTER_DROP_CHANCE) {
				event.getDrops().add(drop);
			}
		}
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public static void onPlayerRenderPre(RenderPlayerEvent.Pre event) {
		if (Mods.BAUBLES.isLoaded() && !BaubleRenderDispatcher.getRegistry().contains(event.getRenderer())) {
			event.getRenderer().addLayer(new BaubleRenderDispatcher(event.getRenderer()));
			BaubleRenderDispatcher.getRegistry().add(event.getRenderer());
		}
	}

	@SideOnly(Side.SERVER)
	@SubscribeEvent
	public void onPlayerLogin(PlayerLoggedInEvent e) {
		if (e.player instanceof EntityPlayerMP) {
			final PacketConfigSync p = new PacketConfigSync(LibConfig.WT_MAX_POWER, LibConfig.WT_BOOSTER_ENABLED);
			LibNetworking.instance().sendTo((WTPacket) p, (EntityPlayerMP) e.player);
		}
	}

	@SubscribeEvent
	public static void tickEvent(TickEvent.PlayerTickEvent e) {
		EntityPlayer player = e.player;
		if (!(player instanceof EntityPlayerMP)) {
			return;
		}
		InventoryPlayer playerInv = player.inventory;
		List<Pair<Boolean, Pair<Integer, ItemStack>>> terminals = WTUtils.getAllWirelessTerminals(player);
		ItemStack wirelessTerm = ItemStack.EMPTY;
		boolean isBauble = false;
		int wctSlot = -1;
		for (int i = 0; i < terminals.size(); i++) {
			if (WTUtils.shouldConsumeBoosters(terminals.get(i).getRight().getRight())) {
				wirelessTerm = terminals.get(i).getRight().getRight();
				isBauble = terminals.get(i).getLeft();
				wctSlot = terminals.get(i).getRight().getLeft();
				break;
			}
		}
		int invSize = playerInv.getSizeInventory();
		if (invSize <= 0) {
			return;
		}
		if (!LibConfig.USE_OLD_INFINTY_MECHANIC && !wirelessTerm.isEmpty() && WTUtils.shouldConsumeBoosters(wirelessTerm)) {
			for (int currentSlot = 0; currentSlot < invSize; currentSlot++) {
				ItemStack slotStack = playerInv.getStackInSlot(currentSlot);
				if (!slotStack.isEmpty() && slotStack.getItem() == LibItems.BOOSTER_CARD) {
					playerInv.setInventorySlotContents(currentSlot, WTUtils.addInfinityBoosters(wirelessTerm, slotStack));
					LibNetworking.instance().sendToDimension(new PacketSyncInfinityEnergyInv(WTUtils.getInfinityEnergy(wirelessTerm), player.getUniqueID(), isBauble, wctSlot), player.getEntityWorld().provider.getDimension());
				}
			}
		}
	}

}