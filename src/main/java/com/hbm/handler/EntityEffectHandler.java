package com.hbm.handler;

import com.hbm.config.RadiationConfig;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.packet.AuxParticlePacketNT;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.ExtPropPacket;
import com.hbm.saveddata.AuxSavedData;
import com.hbm.saveddata.RadiationSavedData;
import com.hbm.util.ContaminationUtil;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class EntityEffectHandler {

	public static void onUpdate(EntityLivingBase entity) {
		
		if(!entity.worldObj.isRemote && entity instanceof EntityPlayerMP) {
			NBTTagCompound data = new NBTTagCompound();
			HbmLivingProps props = HbmLivingProps.getData(entity);
			props.saveNBTData(data);
			PacketDispatcher.wrapper.sendTo(new ExtPropPacket(data), (EntityPlayerMP) entity);
		}
		
		handleRadiation(entity);
		handleDigamma(entity);
	}
	
	private static void handleRadiation(EntityLivingBase entity) {
		
		World world = entity.worldObj;
		
		if(world.isRemote)
			return;
		
		if(entity instanceof EntityPlayer && ((EntityPlayer)entity).capabilities.isCreativeMode)
			return;

		RadiationSavedData data = RadiationSavedData.getData(world);
		int ix = (int)MathHelper.floor_double(entity.posX);
		int iy = (int)MathHelper.floor_double(entity.posY);
		int iz = (int)MathHelper.floor_double(entity.posZ);

		Chunk chunk = world.getChunkFromBlockCoords(ix, iz);
		float rad = data.getRadNumFromCoord(chunk.xPosition, chunk.zPosition);

		if(world.provider.isHellWorld && RadiationConfig.hellRad > 0 && rad < RadiationConfig.hellRad)
			rad = RadiationConfig.hellRad;

		if(rad > 0) {
			ContaminationUtil.applyRadData(entity, rad / 20F);
		}

		if(entity.worldObj.isRaining() && RadiationConfig.cont > 0 && AuxSavedData.getThunder(entity.worldObj) > 0 && entity.worldObj.canBlockSeeTheSky(ix, iy, iz)) {

			ContaminationUtil.applyRadData(entity, RadiationConfig.cont * 0.0005F);
		}
		
		if(HbmLivingProps.getRadiation(entity) > 600 && world.getTotalWorldTime() % 600 == 0) {
			
			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setString("type", "bloodvomit");
			nbt.setInteger("entity", entity.getEntityId());
			PacketDispatcher.wrapper.sendToAllAround(new AuxParticlePacketNT(nbt, 0, 0, 0),  new TargetPoint(entity.dimension, entity.posX, entity.posY, entity.posZ, 25));
			
			world.playSoundEffect(ix, iy, iz, "hbm:entity.vomit", 1.0F, 1.0F);
			entity.addPotionEffect(new PotionEffect(Potion.hunger.id, 60, 19));
		} else if(HbmLivingProps.getRadiation(entity) > 200 && world.getTotalWorldTime() % 1200 == 0) {
			
			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setString("type", "vomit");
			nbt.setInteger("entity", entity.getEntityId());
			PacketDispatcher.wrapper.sendToAllAround(new AuxParticlePacketNT(nbt, 0, 0, 0),  new TargetPoint(entity.dimension, entity.posX, entity.posY, entity.posZ, 25));
			
			world.playSoundEffect(ix, iy, iz, "hbm:entity.vomit", 1.0F, 1.0F);
			entity.addPotionEffect(new PotionEffect(Potion.hunger.id, 60, 19));
		
		}
	}
	
	private static void handleDigamma(EntityLivingBase entity) {
		
		if(!entity.worldObj.isRemote) {
			
			float digamma = HbmLivingProps.getDigamma(entity);
			
			if(digamma == 0)
				return;
			
			int chance = Math.max(10 - (int)(digamma), 1);
			
			if(chance == 1 || entity.getRNG().nextInt(chance) == 0) {
				
				NBTTagCompound data = new NBTTagCompound();
				data.setString("type", "digammaDecay");
				data.setInteger("entity", entity.getEntityId());
				PacketDispatcher.wrapper.sendToAllAround(new AuxParticlePacketNT(data, 0, 0, 0),  new TargetPoint(entity.dimension, entity.posX, entity.posY, entity.posZ, 25));
			}
		}
	}
}
