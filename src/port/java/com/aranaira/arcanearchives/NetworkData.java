package com.aranaira.arcanearchives;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.*;

/** Only loaded chunks are queried. No world/chunk references are retained. */
public final class NetworkData extends SavedData {
    public record Node(ResourceLocation dimension, BlockPos pos, UUID owner,String kind) {}
    public record Entry(ItemStack stack, long count) {}
    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private final Map<UUID, UUID> members = new HashMap<>();
    private final Set<UUID> receivedBooks=new HashSet<>();
    private final Map<UUID,Boolean> trovePreferences=new HashMap<>(),routingPreferences=new HashMap<>();
    public boolean trovesDispense(UUID owner){return trovePreferences.getOrDefault(owner,true);}
    public boolean defaultNoNew(UUID owner){return routingPreferences.getOrDefault(owner,false);}
    public void preferences(UUID player,boolean trovesDispense,boolean noNew){trovePreferences.put(player,trovesDispense);routingPreferences.put(player,noNew);setDirty();}
    public boolean grantBook(UUID player){boolean first=receivedBooks.add(player);if(first)setDirty();return first;}
    public static NetworkData get(Level level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(NetworkData::load, NetworkData::new, "arcanearchives_networks");
    }
    public UUID network(UUID player) { return members.getOrDefault(player, player); }
    public boolean sameNetwork(UUID a, UUID b) { return a != null && b != null && network(a).equals(network(b)); }
    public List<UUID> members(UUID player){
        UUID root=network(player);List<UUID> result=new ArrayList<>();result.add(root);
        members.forEach((member,owner)->{if(owner.equals(root))result.add(member);});return List.copyOf(result);
    }
    public int rebuild(MinecraftServer server,UUID player){
        int count=0;
        for(Node node:List.copyOf(nodes(player))){
            var level=server.getLevel(ResourceKey.create(Registries.DIMENSION,node.dimension()));
            if(level==null||!level.hasChunkAt(node.pos()))continue;
            if(level.getBlockEntity(node.pos()) instanceof ArchiveBlockEntity archive&&sameNetwork(archive.owner,player)){register(archive);count++;}
            else{nodes.remove(key(node));setDirty();}
        }
        return count;
    }
    public boolean join(UUID issuer, UUID member) {
        if (!network(issuer).equals(issuer) || member.equals(issuer) || members.containsKey(member)||members.containsValue(member)) return false;
        members.put(member, issuer); setDirty(); return true;
    }
    public void leave(UUID member) { members.remove(member); setDirty(); }
    public boolean expel(UUID issuer, UUID member) { if (!issuer.equals(members.get(member)))return false;leave(member);return true; }
    private String key(Node n) { return n.dimension() + ":" + n.pos().asLong(); }
    public void register(ArchiveBlockEntity tile) {
        if (tile.owner == null || tile.getLevel() == null) return;
        register(new Node(tile.getLevel().dimension().location(), tile.getBlockPos().immutable(), tile.owner,tile.kind()));
    }
    public void register(Node node) { if (!node.equals(nodes.put(key(node), node))) setDirty(); }
    public void unregister(ArchiveBlockEntity tile) {
        if (tile.getLevel() != null && nodes.remove(tile.getLevel().dimension().location() + ":" + tile.getBlockPos().asLong()) != null) setDirty();
    }
    public List<Node> nodes(UUID owner) {
        return nodes.values().stream().filter(n -> sameNetwork(n.owner(), owner)).toList();
    }
    public List<IItemHandler> inventories(MinecraftServer server, UUID owner, BlockPos exclude) {
        return inventories(server,owner,(ArchiveBlockEntity)null);
    }
    private List<IItemHandler> inventories(MinecraftServer server,UUID owner,ArchiveBlockEntity source){
        return inventories(server,owner,source,nodes(owner));
    }
    private List<IItemHandler> inventories(MinecraftServer server,UUID owner,ArchiveBlockEntity source,List<Node> selected){
        List<IItemHandler> result = new ArrayList<>();
        Set<String> seen=new HashSet<>();
        for (Node node : selected) {
            if(source!=null&&(!node.dimension().equals(source.getLevel().dimension().location())||
                    Math.pow(node.pos().getX()-source.getBlockPos().getX(),2)+Math.pow(node.pos().getZ()-source.getBlockPos().getZ(),2)>source.routingRadius*source.routingRadius))continue;
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, node.dimension()));
            if (level == null || !level.hasChunkAt(node.pos())) continue;
            var tile = level.getBlockEntity(node.pos());
            if (tile instanceof ArchiveBlockEntity archive) {
                if (!sameNetwork(archive.owner, owner) || !Set.of("radiant_chest","radiant_trove","monitoring_crystal","gemcutters_table","radiant_crafting_table").contains(archive.kind())) continue;
                if (archive.kind().equals("monitoring_crystal")) {
                    BlockPos target = node.pos().relative(archive.getBlockState().getValue(ArchiveBlock.FACING).getOpposite());
                    if (!level.hasChunkAt(target)) continue;
                    var other = level.getBlockEntity(target);
                    if (other != null && !(other instanceof ArchiveBlockEntity) && seen.add(node.dimension()+":"+target.asLong())) other.getCapability(ForgeCapabilities.ITEM_HANDLER, archive.getBlockState().getValue(ArchiveBlock.FACING)).ifPresent(result::add);
                } else result.add(archive.inventory);
            }
        }
        return result;
    }
    public record LocationDetail(Node node,String label,long count){}
    private String physicalStorage(MinecraftServer server,Node node){
        var pos=node.pos();if(node.kind().equals("monitoring_crystal")){var level=server.getLevel(ResourceKey.create(Registries.DIMENSION,node.dimension()));if(level!=null&&level.getBlockEntity(pos) instanceof ArchiveBlockEntity archive)pos=pos.relative(archive.getBlockState().getValue(ArchiveBlock.FACING).getOpposite());}
        return node.dimension()+":"+pos.asLong();
    }
    public List<LocationDetail> locationDetails(MinecraftServer server,UUID owner,ItemStack template){
        var result=new ArrayList<LocationDetail>();var seen=new HashSet<String>();for(var node:nodes(owner)){
            var handlers=inventories(server,owner,null,List.of(node));if(handlers.isEmpty()||!seen.add(physicalStorage(server,node)))continue;
            long count=0;for(var handler:handlers)for(int i=0;i<handler.getSlots();i++){var s=handler.getStackInSlot(i);if(ItemStack.isSameItemSameTags(s,template))count+=s.getCount();}
            if(count==0)continue;var level=server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,node.dimension()));String label=node.kind();
            if(level!=null&&level.hasChunkAt(node.pos()))label=level.getBlockEntity(node.pos()) instanceof ArchiveBlockEntity t?t.getDisplayName().getString():level.getBlockState(node.pos()).getBlock().getName().getString();
            result.add(new LocationDetail(node,label,count));if(result.size()>=16)break;
        }return result;
    }
    public List<Node> locations(MinecraftServer server,UUID owner,ItemStack template){
        List<Node> result=new ArrayList<>();
        var seen=new HashSet<String>();
        for(Node node:nodes(owner)){
            var handlers=inventories(server,owner,null,List.of(node));if(handlers.isEmpty()||!seen.add(physicalStorage(server,node)))continue;
            boolean found=false;
            for(IItemHandler handler:handlers){
                for(int i=0;i<handler.getSlots();i++)if(ItemStack.isSameItemSameTags(handler.getStackInSlot(i),template)){found=true;break;}
                if(found)break;
            }
            if(found)result.add(node);
            if(result.size()>=1024)break;
        }
        return result;
    }
    public List<Entry> entries(MinecraftServer server, UUID owner) {
        return collate(inventories(server,owner,(BlockPos)null));
    }
    public static List<Entry> collate(List<IItemHandler> handlers){
        Map<StackIdentity,Entry> grouped=new LinkedHashMap<>();
        for (IItemHandler handler : handlers) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (stack.isEmpty()) continue;
                var key=new StackIdentity(stack);Entry prior=grouped.get(key);
                grouped.put(key,prior==null?new Entry(stack.copyWithCount(1),stack.getCount()):new Entry(prior.stack(),prior.count()+stack.getCount()));
            }
        }
        List<Entry> result=new ArrayList<>(grouped.values());result.sort(Comparator.comparing(e -> e.stack().getHoverName().getString()));
        return result;
    }
    public ItemStack insert(MinecraftServer server, UUID owner, ItemStack input, BlockPos exclude) {
        return route(inventories(server,owner,(BlockPos)null),input,false);
    }
    public ItemStack insertFromBrazier(ArchiveBlockEntity source,ItemStack input,boolean simulate){
        if(source.owner==null||source.getLevel()==null||source.getLevel().isClientSide)return input;
        ItemStack remaining=input.copy();
        for(var tile:brazierTargets(source,input)){
            if(tile.kind().equals("gemcutters_table")){
                for(int i=0;i<tile.inventory.getSlots()&&!remaining.isEmpty();i++)
                    if(ItemStack.isSameItemSameTags(tile.inventory.getStackInSlot(i),remaining))remaining=tile.inventory.insertItem(i,remaining,simulate);
            }else remaining=ItemHandlerHelper.insertItemStacked(tile.inventory,remaining,simulate);
            if(remaining.isEmpty())break;
        }
        return remaining;
    }
    /** Source weights: Gemcutter 5000, no-new chest 4999, Trove 4000–4700. */
    public List<ArchiveBlockEntity> brazierTargets(ArchiveBlockEntity source,ItemStack input){
        if(!(source.getLevel() instanceof ServerLevel level)||source.owner==null)return List.of();
        List<ArchiveBlockEntity> targets=new ArrayList<>();
        for(Node node:nodes(source.owner)){
            if(!node.dimension().equals(level.dimension().location())||source.subnetworkOnly&&!node.owner().equals(source.owner))continue;
            long dx=(long)node.pos().getX()-source.getBlockPos().getX(),dz=(long)node.pos().getZ()-source.getBlockPos().getZ();
            if(dx*dx+dz*dz>(long)source.routingRadius*source.routingRadius||!level.hasChunkAt(node.pos()))continue;
            if(level.getBlockEntity(node.pos()) instanceof ArchiveBlockEntity tile&&sameNetwork(tile.owner,source.owner)
                    &&(!source.subnetworkOnly||source.owner.equals(tile.owner))&&routingWeight(tile,input)>=0)targets.add(tile);
        }
        targets.sort(Comparator.comparingInt((ArchiveBlockEntity t)->routingWeight(t,input)).reversed());
        return targets;
    }
    public static int routingWeight(ArchiveBlockEntity tile,ItemStack input){
        if(tile.busy)return -1;
        if(!Set.of("radiant_chest","radiant_trove","gemcutters_table").contains(tile.kind()))return -1;
        int occupied=0;long matching=0;
        for(int i=0;i<tile.inventory.getSlots();i++){
            ItemStack stack=tile.inventory.getStackInSlot(i);if(!stack.isEmpty())occupied++;
            if(ItemStack.isSameItemSameTags(stack,input))matching+=stack.getCount();
        }
        if(tile.kind().equals("radiant_trove")){
            if(!ItemStack.isSameItemSameTags(tile.reference,input))return -1;
            return !tile.voiding?4500:tile.inventory.getStackInSlot(0).getCount()<tile.inventory.getSlotLimit(0)?4700:4000;
        }
        if(tile.kind().equals("gemcutters_table"))return matching==0?-1:5000;
        if(tile.noNew)return matching==0?-1:4999;
        if(occupied==0)return 0;
        if(matching==0)return occupied;
        int capacity=input.getMaxStackSize()==1?1:input.getMaxStackSize()*ArchiveConfig.CHEST_MULTIPLIER.get();
        return (int)Math.ceil((double)matching/(capacity*tile.inventory.getSlots())*1000+500);
    }
    private ItemStack route(List<IItemHandler> handlers,ItemStack input,boolean simulate){
        ItemStack rest = input.copy();
        // Fill matching slots before opening new slots, including across containers.
        for (boolean existing : new boolean[]{true,false}) {
            for (IItemHandler handler : handlers) {
                for (int slot = 0; slot < handler.getSlots() && !rest.isEmpty(); slot++) {
                    if(!existing && handler instanceof BulkInventory bulk && !bulk.acceptsNew())continue;
                    ItemStack present = handler.getStackInSlot(slot);
                    if (present.isEmpty() == existing) continue;
                    rest = handler.insertItem(slot, rest, simulate);
                }
            }
        }
        return rest;
    }
    public ItemStack extract(MinecraftServer server, UUID owner, ItemStack template, int amount) {
        ItemStack result = ItemStack.EMPTY;
        int wanted = Math.min(amount, template.getMaxStackSize());
        for (IItemHandler handler : inventories(server, owner, (BlockPos)null)) {
            for (int slot = 0; slot < handler.getSlots() && wanted > 0; slot++) {
                ItemStack present = handler.getStackInSlot(slot);
                if (!ItemStack.isSameItemSameTags(present, template)) continue;
                ItemStack extracted = handler.extractItem(slot, wanted, false);
                if (extracted.isEmpty()) continue;
                if (result.isEmpty()) result = extracted; else result.grow(extracted.getCount());
                wanted -= extracted.getCount();
            }
        }
        return result;
    }
    public static NetworkData load(CompoundTag tag) {
        NetworkData data = new NetworkData();
        ListTag list = tag.getList("Nodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag n = list.getCompound(i);
            ResourceLocation dim = ResourceLocation.tryParse(n.getString("Dimension"));
            if (dim != null && n.hasUUID("Owner")) {
                Node node = new Node(dim, BlockPos.of(n.getLong("Position")), n.getUUID("Owner"),n.getString("Kind"));
                data.nodes.put(data.key(node), node);
            }
        }
        ListTag members = tag.getList("Members", Tag.TAG_COMPOUND);
        for (int i = 0; i < members.size(); i++) {
            CompoundTag n = members.getCompound(i);
            if (n.hasUUID("Member") && n.hasUUID("Owner")) data.members.put(n.getUUID("Member"), n.getUUID("Owner"));
        }
        ListTag books=tag.getList("Books",Tag.TAG_COMPOUND);
        for(int i=0;i<books.size();i++){var book=books.getCompound(i);if(book.hasUUID("Player"))data.receivedBooks.add(book.getUUID("Player"));}
        var preferences=tag.getList("Preferences",Tag.TAG_COMPOUND);for(int i=0;i<preferences.size();i++){var entry=preferences.getCompound(i);if(entry.hasUUID("Player")){var player=entry.getUUID("Player");data.trovePreferences.put(player,entry.getBoolean("TrovesDispense"));data.routingPreferences.put(player,entry.getBoolean("NoNew"));}}
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Node n : nodes.values()) {
            CompoundTag t = new CompoundTag(); t.putString("Dimension",n.dimension().toString()); t.putLong("Position",n.pos().asLong()); t.putUUID("Owner",n.owner());t.putString("Kind",n.kind()); list.add(t);
        }
        tag.put("Nodes",list);
        ListTag membership = new ListTag();
        members.forEach((member,owner) -> { CompoundTag t = new CompoundTag(); t.putUUID("Member",member); t.putUUID("Owner",owner); membership.add(t); });
        tag.put("Members",membership);
        ListTag books=new ListTag();for(var player:receivedBooks){var book=new CompoundTag();book.putUUID("Player",player);books.add(book);}tag.put("Books",books);
        var preferences=new ListTag();var players=new HashSet<UUID>(trovePreferences.keySet());players.addAll(routingPreferences.keySet());for(var player:players){var entry=new CompoundTag();entry.putUUID("Player",player);entry.putBoolean("TrovesDispense",trovesDispense(player));entry.putBoolean("NoNew",defaultNoNew(player));preferences.add(entry);}tag.put("Preferences",preferences);
        return tag;
    }
}
