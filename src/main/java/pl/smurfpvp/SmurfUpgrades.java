package pl.smurfpvp;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class SmurfUpgrades extends JavaPlugin implements Listener, CommandExecutor {
  private final String[] types={"helmet","chestplate","leggings","boots","sword","axe","shears","elytra","pickaxe"};
  private final String[] labels={"Główka","Klata","Spodnie","Buty","Miecz","Siekiera","Nożyce","Elytra","Kilof"};
  private final Material[] materials={Material.PLAYER_HEAD,Material.LEATHER_CHESTPLATE,Material.LEATHER_LEGGINGS,Material.LEATHER_BOOTS,Material.DIAMOND_SWORD,Material.NETHERITE_AXE,Material.SHEARS,Material.ELYTRA,Material.NETHERITE_PICKAXE};
  private final int[] categorySlots={19,20,21,22,23,24,25,31,32};
  private Economy economy; private FileConfiguration upgrades,gui,players;
  private File playersFile; private final Map<UUID,Menu> open=new HashMap<>(); private final Map<UUID,Inventory> menuInventories=new HashMap<>();
  private record Menu(String kind,int type,int level) {}
  private NamespacedKey ownedKey;
  private final Set<String> warnedHeads=new HashSet<>();

  @Override public void onEnable(){saveDefaultConfig(); ensure("upgrades.yml");ensure("gui.yml");playersFile=new File(getDataFolder(),"players.yml");players=YamlConfiguration.loadConfiguration(playersFile);loadOther();ownedKey=new NamespacedKey(this,"avengers_item");
    if(getServer().getPluginManager().getPlugin("Vault")!=null){RegisteredServiceProvider<Economy> p=getServer().getServicesManager().getRegistration(Economy.class);if(p!=null)economy=p.getProvider();}
    for(String c:new String[]{"ulepsz","sety","cenyulepszen","smurfadmin","avengersadmin","topulepszen"}) Objects.requireNonNull(getCommand(c)).setExecutor(this);
    getServer().getPluginManager().registerEvents(this,this);
    getLogger().info("AvengersUpgrades enabled; Vault economy: "+(economy!=null));
  }
  private void ensure(String name){File f=new File(getDataFolder(),name);if(!f.exists())saveResource(name,false);}
  private void loadOther(){warnedHeads.clear();reloadConfig();upgrades=YamlConfiguration.loadConfiguration(new File(getDataFolder(),"upgrades.yml"));gui=YamlConfiguration.loadConfiguration(new File(getDataFolder(),"gui.yml"));}
  private void savePlayers(){try{players.save(playersFile);}catch(Exception e){getLogger().severe("Cannot save players.yml: "+e.getMessage());}}
  private String path(Player p,int type){return "players."+p.getUniqueId()+"."+types[type];}
  private int level(Player p,int type){return Math.max(0,Math.min(12,players.getInt(path(p,type),0)));}
  private void init(Player p){boolean changed=false;for(int i=0;i<types.length;i++){if(!players.contains(path(p,i))){players.set(path(p,i),0);changed=true;}}if(changed)savePlayers();}
  private String tr(String s){
    if(s==null)return "";
    java.util.regex.Matcher m=java.util.regex.Pattern.compile("&#([0-9a-fA-F]{6})").matcher(s);
    StringBuffer out=new StringBuffer();
    while(m.find())m.appendReplacement(out,java.util.regex.Matcher.quoteReplacement(net.md_5.bungee.api.ChatColor.of("#"+m.group(1)).toString()));
    m.appendTail(out);
    return ChatColor.translateAlternateColorCodes('&',out.toString());
  }
  private String title(String name){return tr("&8⚡ "+name);}
  private String character(int l){return upgrades.getString("levels."+l+".name","Poziom "+l);}
  private double price(int type,int l){return Math.max(0,upgrades.getDouble("items."+types[type]+".levels."+l+".price",upgrades.getDouble("levels."+l+".price",0)));}
  private double multiplier(int chance){return getConfig().getDouble("chances."+chance+".price-multiplier",chance/100.0);}
  private ItemStack simple(Material m,String name,String... lore){ItemStack i=new ItemStack(m);ItemMeta meta=i.getItemMeta();meta.setDisplayName(tr(name));meta.setLore(Arrays.stream(lore).map(this::tr).toList());i.setItemMeta(meta);return i;}
  private int guiSlot(String key,int fallback){return gui.getInt("slots."+key,fallback);}
  private void put(Inventory inv,int slot,ItemStack item){if(slot>=0&&slot<inv.getSize())inv.setItem(slot,item);}
  private Inventory inventory(String name){Inventory inv=Bukkit.createInventory(null,54,title(name));Material glass=Material.matchMaterial(gui.getString("background","GRAY_STAINED_GLASS_PANE"));if(glass==null)glass=Material.GRAY_STAINED_GLASS_PANE;ItemStack filler=simple(glass," ");for(int s=0;s<54;s++)inv.setItem(s,filler);return inv;}
  private void display(Player p,Inventory inv,String kind,int type,int l){open.put(p.getUniqueId(),new Menu(kind,type,l));menuInventories.put(p.getUniqueId(),inv);p.openInventory(inv);}
  private String rankColor(int l){String[] colors={"&e","&b","&d","&9","&c","&f","&6","&5","&a","&7","&4","&d"};return gui.getString("levels."+l+".color",colors[Math.max(0,Math.min(11,l-1))]);}
  private String levelTag(int l){return tr(rankColor(l)+gui.getString("level-format","⭐{level}").replace("{level}",String.valueOf(l)));}
  private String format(String input,int l){return tr(input.replace("{level}",String.valueOf(l)).replace("{stars}",levelTag(l)).replace("{character}",character(l)));}
  private void effect(Player p,boolean success,int l){
    String base=success?"effects.success":"effects.failure";
    if(!getConfig().getBoolean(base+".enabled",true))return;
    String soundName=getConfig().getString(base+".sound",success?"ENTITY_PLAYER_LEVELUP":"ENTITY_VILLAGER_NO");
    try{p.playSound(p.getLocation(),soundName.toLowerCase(Locale.ROOT).replace('_','.'),1f,success?1.2f:1f);}catch(IllegalArgumentException ex){getLogger().warning("Unknown sound: "+soundName);}
    String particleName=getConfig().getString(base+".particle",success?"HAPPY_VILLAGER":"SMOKE");
    try{Particle particle=Particle.valueOf(particleName);if(particle.getDataType()==Void.class)p.getWorld().spawnParticle(particle,p.getLocation().add(0,1.2,0),getConfig().getInt(base+".count",24),.5,.7,.5,.04);}catch(IllegalArgumentException ex){getLogger().warning("Unknown particle: "+particleName);}
    if(success&&getConfig().getBoolean(base+".title-enabled",true))p.sendTitle(format(getConfig().getString(base+".title","&a&lULEPSZONO!"),l),format(getConfig().getString(base+".subtitle","&fNowy poziom: {stars}"),l),5,35,10);
    if(success&&l==12&&getConfig().getBoolean("legendary.broadcast",true))Bukkit.broadcastMessage(format(getConfig().getString("legendary.broadcast-message","&5✦ &d{player} &7odblokował poziom &d{stars}&7!"),l).replace("{player}",p.getName()));
  }
  private void ranking(Player p){
    int limit=Math.max(1,Math.min(50,getConfig().getInt("ranking.limit",10)));
    String prefix="players.";
    var section=players.getConfigurationSection("players");
    if(section==null){p.sendMessage(tr("&7Ranking jest pusty."));return;}
    record Entry(String name,int total){}
    List<Entry> entries=new ArrayList<>();
    for(String key:section.getKeys(false)){
      try{UUID uuid=UUID.fromString(key);int total=0;for(String type:types)total+=Math.max(0,Math.min(12,players.getInt(prefix+key+"."+type,0)));
        String name=Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName()).orElse(key.substring(0,8));entries.add(new Entry(name,total));
      }catch(IllegalArgumentException ignored){}
    }
    entries.sort(Comparator.comparingInt(Entry::total).reversed().thenComparing(Entry::name));
    p.sendMessage(tr(getConfig().getString("ranking.header","&b&l✦ TOP ULEPSZEŃ ✦")));
    for(int i=0;i<Math.min(limit,entries.size());i++){Entry entry=entries.get(i);p.sendMessage(tr("&e"+(i+1)+". &f"+entry.name()+" &8» &b"+entry.total()+" &7/ "+(types.length*12)+" ⭐"));}
  }
  private void main(Player p){
    Inventory inv=inventory("&b&lULEPSZENIA PRZEDMIOTÓW");
    // Symetryczna ramka i dyskretne dekoracje. Sloty kategorii są konfigurowalne.
    ItemStack edge=simple(Material.BLUE_STAINED_GLASS_PANE," ");
    ItemStack accent=simple(Material.LIGHT_BLUE_STAINED_GLASS_PANE," ");
    ItemStack dark=simple(Material.BLACK_STAINED_GLASS_PANE," ");
    for(int i=0;i<54;i++)put(inv,i,dark);
    for(int i=0;i<9;i++){put(inv,i,edge);put(inv,45+i,edge);}
    for(int row=1;row<5;row++){put(inv,row*9,edge);put(inv,row*9+8,edge);}
    for(int i:new int[]{2,3,5,6,10,16,28,34,38,42,47,51})put(inv,i,accent);
    
    for(int t=0;t<types.length;t++){
      int l=level(p,t);
      String color=rankColor(l);
      ItemStack item=l==0?new ItemStack(materials[t]):equipment(t,l,false);
      ItemMeta meta=item.getItemMeta();
      meta.setDisplayName(tr(color+"&l✦ "+labels[t]+" &8• ")+(l==0?tr("&70 ⭐"):levelTag(l)));
      meta.setLore(Arrays.asList(tr("&8━━━━━━━━━━━━━━━━━━━━"),tr("&7Postać: "+(l==0?"&8Nieodblokowana":color+character(l))),tr("&7Twój poziom: ")+(l==0?tr("&70 ⭐"):levelTag(l))+tr(" &8/ &f12"),tr("&7Następny: ")+(l==12?tr("&dMAX"):levelTag(l+1)),tr(" "),tr("&e▸ LPM &7— ulepsz przedmiot"),tr("&b▸ PPM &7— wszystkie poziomy"),tr("&8━━━━━━━━━━━━━━━━━━━━")));
      item.setItemMeta(meta);
      put(inv,guiSlot("category."+types[t],categorySlots[t]),item);
    }
    // Usunięto książkę informacyjną z GUI /ulepsz.
    put(inv,guiSlot("back",49),simple(Material.BARRIER,"&c&l✖ ZAMKNIJ","&7Kliknij, aby wyjść"));
    display(p,inv,"main",-1,0);
  }
  private void upgrade(Player p,int type){int current=level(p,type);Inventory inv=inventory("ULEPSZ: "+labels[type]);
    for(int chance:new int[]{25,50,100}){int slot=guiSlot("chance."+chance,chance==25?11:chance==50?13:15);int next=Math.min(12,current+1);double cost=current==0?0:price(type,next)*multiplier(chance);put(inv,slot,simple(chance==25?Material.ORANGE_CONCRETE:chance==50?Material.YELLOW_CONCRETE:Material.LIME_CONCRETE,"&e"+(current==0?"100":chance)+"% szansy","&7Cena próby: &6"+money(cost)+" $","&7Kliknij, aby spróbować"));}
    put(inv,guiSlot("current",21),current==0?simple(Material.BARRIER,"&7Brak przedmiotu","&aKup ⭐1 za 0 $"):equipment(type,current,false));put(inv,guiSlot("next",23),current==12?simple(Material.NETHER_STAR,"&dMAX POZIOM"):equipment(type,current+1,false));put(inv,guiSlot("back",49),simple(Material.ARROW,"&cWróć"));display(p,inv,"upgrade",type,current);}
  private String money(double d){return String.format(Locale.US,"%,.0f",d);}
  private void preview(Player p,int type){Inventory inv=inventory("POZIOMY: "+labels[type]);for(int l=1;l<=12;l++)put(inv,10+l-1,equipment(type,l,false));put(inv,guiSlot("back",49),simple(Material.ARROW,"&cWróć"));display(p,inv,"preview",type,0);}
  private void sets(Player p){Inventory inv=inventory("WSZYSTKIE SETY");for(int l=1;l<=12;l++)put(inv,10+l-1,head(l,"&f"+l+". "+character(l),"&7Kliknij: pełny podgląd","&7Koszt tego poziomu: &6"+money(total(l))+" $"));put(inv,guiSlot("back",49),simple(Material.BARRIER,"&cZamknij"));display(p,inv,"sets",-1,0);}
  private double total(int l){double sum=0;for(int t=0;t<types.length;t++)sum+=price(t,l);return sum;}
  private void setPreview(Player p,int l){Inventory inv=inventory("SET "+l+": "+character(l));for(int t=0;t<types.length;t++)put(inv,categorySlots[t],equipment(t,l,false));put(inv,31,simple(Material.GOLD_INGOT,"&6Cena pełnego poziomu: "+money(total(l))+" $","&7Łączny koszt od I do "+l+": &e"+money(cumulative(l))+" $"));put(inv,guiSlot("back",49),simple(Material.ARROW,"&cWróć"));display(p,inv,"setpreview",-1,l);}
  private double cumulative(int l){double sum=0;for(int j=2;j<=l;j++)sum+=total(j);return sum;}
  private void costs(Player p){Inventory inv=inventory("CENNIK SETÓW");for(int l=1;l<=12;l++)put(inv,10+l-1,simple(Material.GOLD_INGOT,"&e"+l+". "+character(l),"&7Cena przejścia wszystkich części: &6"+money(total(l))+" $","&7Łącznie od poziomu 1: &6"+money(cumulative(l))+" $"));put(inv,guiSlot("back",49),simple(Material.BARRIER,"&cZamknij"));display(p,inv,"costs",-1,0);}
  private ItemStack head(int l,String name,String... lore){String id=upgrades.getString("levels."+l+".head-id","");ItemStack item=null;
    if(!id.isBlank()&&Bukkit.getPluginManager().isPluginEnabled("HeadDatabase")){try{Class<?> c=Class.forName("me.arcaniax.hdb.api.HeadDatabaseAPI");Object api=c.getDeclaredConstructor().newInstance();Method m=c.getMethod("getItemHead",String.class);Object got=m.invoke(api,id);if(got instanceof ItemStack stack&&stack.getType()==Material.PLAYER_HEAD)item=stack.clone();}catch(Exception e){if(warnedHeads.add(id))getLogger().warning("HeadDatabase ID "+id+" unavailable: "+e.getMessage());}}
    if(item==null){
      item=new ItemStack(Material.PLAYER_HEAD);
      String texture=upgrades.getString("levels."+l+".texture-base64", "");
      if(texture.isBlank() && id.isBlank() && warnedHeads.add("missing-"+l))getLogger().warning("Brak tekstury glowy Avengers poziom "+l+" ("+character(l)+"). Ustaw levels."+l+".head-id w upgrades.yml po /hdb search.");
      if(!texture.isBlank() && item.getItemMeta() instanceof SkullMeta skull){
        try{
          PlayerProfile profile=Bukkit.createProfile(UUID.nameUUIDFromBytes(("avengers-head-"+l).getBytes(StandardCharsets.UTF_8)));
          profile.setProperty(new ProfileProperty("textures",texture));
          skull.setPlayerProfile(profile);item.setItemMeta(skull);
        }catch(Exception e){getLogger().warning("Nieprawidłowa tekstura poziomu "+l+": "+e.getMessage());}
      }
    }ItemMeta meta=item.getItemMeta();meta.setDisplayName(tr(name));meta.setLore(Arrays.stream(lore).map(this::tr).toList());item.setItemMeta(meta);return item;}
  private String small(String value){
    String from="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    String to="ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ";
    StringBuilder out=new StringBuilder();
    for(char c:value.toLowerCase(Locale.ROOT).toCharArray()){
      int i="abcdefghijklmnopqrstuvwxyz".indexOf(c);
      out.append(i>=0?new String[]{"ᴀ","ʙ","ᴄ","ᴅ","ᴇ","ꜰ","ɢ","ʜ","ɪ","ᴊ","ᴋ","ʟ","ᴍ","ɴ","ᴏ","ᴘ","ǫ","ʀ","ꜱ","ᴛ","ᴜ","ᴠ","ᴡ","x","ʏ","ᴢ"}[i]:c);
    }
    return out.toString();
  }
  private String enchantLabel(String key){return switch(key.toLowerCase(Locale.ROOT)){
    case "sharpness"->"Ostrość";case "protection"->"Ochrona";case "unbreaking"->"Niezniszczalność";
    case "efficiency"->"Wydajność";case "fortune"->"Szczęście";case "mending"->"Naprawa";
    case "fire_aspect"->"Zaklęty ogień";case "looting"->"Grabież";case "feather_falling"->"Łagodny upadek";
    case "silk_touch"->"Jedwabny dotyk";case "sweeping_edge"->"Szerokie ostrze";
    default->key.replace('_',' ');
  };}
  private ItemStack equipment(int type,int l,boolean tagged){
    ItemStack item=type==0?head(l," "):new ItemStack(materials[type]);
    ItemMeta meta=item.getItemMeta();
    String color=upgrades.getString("levels."+l+".color","#FFFFFF");
    String prefix=upgrades.getString("levels."+l+".title-color","&f");
    String display=small(labels[type])+" "+small(character(l))+" ⭐"+l;
    meta.setDisplayName(tr(prefix+display));
    if(meta instanceof LeatherArmorMeta leather){try{leather.setColor(Color.fromRGB(Integer.parseInt(color.replace("#",""),16)));}catch(Exception ignored){}}
    String enchPath="items."+types[type]+".levels."+l+".enchants";
    var section=upgrades.getConfigurationSection(enchPath);
    List<String> lore=new ArrayList<>();
    if(section!=null){
      for(String key:section.getKeys(false)){
        Enchantment ench=Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key.toLowerCase(Locale.ROOT)));
        if(ench!=null){
          int value=Math.max(1,Math.min(32767,section.getInt(key)));
          meta.addEnchant(ench,value,true);
          String tint=key.equalsIgnoreCase("unbreaking")?"&a":"&b";
          lore.add(tr(tint+"[✦] "+enchantLabel(key)+" "+value));
        }
      }
    }
    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS,org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
    meta.setLore(lore);
    // CustomModelData is applied only to the plugin's sword, including GUI previews.
    if(type==4 && getConfig().getBoolean("sword-texture.enabled",true)){
      int modelData=getConfig().getInt("sword-texture.custom-model-data",240000)+l;
      if(modelData>0)meta.setCustomModelData(modelData);
    }
    if(tagged)meta.getPersistentDataContainer().set(ownedKey,PersistentDataType.STRING,types[type]);item.setItemMeta(meta);return item;}
  private void give(Player p){grantMissing(p);}
  // Grant only missing categories. Never overwrite unrelated inventory items.
  private void grantMissing(Player p){
    for(int t=0;t<types.length;t++){
      if(hasManaged(p,t))continue;
      ItemStack item=equipment(t,level(p,t),true);
      if(t==0 && empty(p.getInventory().getHelmet()))p.getInventory().setHelmet(item);
      else if(t==1 && empty(p.getInventory().getChestplate()))p.getInventory().setChestplate(item);
      else if(t==2 && empty(p.getInventory().getLeggings()))p.getInventory().setLeggings(item);
      else if(t==3 && empty(p.getInventory().getBoots()))p.getInventory().setBoots(item);
      else {Map<Integer,ItemStack> leftover=p.getInventory().addItem(item);
        for(ItemStack stack:leftover.values())p.getWorld().dropItemNaturally(p.getLocation(),stack);}
    }
    p.updateInventory();
  }
  private boolean empty(ItemStack item){return item==null||item.getType().isAir();}
  private boolean hasManaged(Player p,int type){
    if(type==0 && isManaged(p.getInventory().getHelmet(),type))return true;
    if(type==1 && isManaged(p.getInventory().getChestplate(),type))return true;
    if(type==2 && isManaged(p.getInventory().getLeggings(),type))return true;
    if(type==3 && isManaged(p.getInventory().getBoots(),type))return true;
    for(ItemStack item:p.getInventory().getStorageContents())if(isManaged(item,type))return true;
    return isManaged(p.getInventory().getItemInOffHand(),type);
  }
  // Additional wood drops only for the plugin-managed axe. Does not modify other tools.
  @EventHandler(ignoreCancelled=true, priority=EventPriority.HIGH)
  public void extraWood(BlockBreakEvent e){
    if(!getConfig().getBoolean("axe-wood.enabled",true))return;
    Player p=e.getPlayer();
    if(p.getGameMode()!=GameMode.SURVIVAL)return;
    ItemStack tool=p.getInventory().getItemInMainHand();
    if(!isManaged(tool,5) || tool.getType()!=Material.NETHERITE_AXE || !e.isDropItems())return;
    String block=e.getBlock().getType().name();
    if(!(block.endsWith("_LOG")||block.endsWith("_WOOD")||block.endsWith("_STEM")||block.endsWith("_HYPHAE")))return;
    int l=level(p,5);
    double factor=Math.max(1.0,Math.min(64.0,getConfig().getDouble("axe-wood.levels."+l+".multiplier",1.0)));
    if(factor<=1.0)return;
    // Keep the vanilla drop and add only the bonus; never duplicate XP or other loot.
    Collection<ItemStack> drops=e.getBlock().getDrops(tool,p);
    int whole=(int)Math.floor(factor-1.0);
    double fraction=factor-1.0-whole;
    for(ItemStack drop:drops){
      if(drop.getType().isAir())continue;
      int extra=drop.getAmount()*whole;
      for(int i=0;i<drop.getAmount();i++)if(Math.random()<fraction)extra++;
      while(extra>0){ItemStack bonus=drop.clone();int count=Math.min(extra,bonus.getMaxStackSize());bonus.setAmount(count);e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(),bonus);extra-=count;}
    }
  }
  // Pickaxe bonus only on natural ore blocks; optional and disabled by default to prevent farm abuse.
  @EventHandler(ignoreCancelled=true, priority=EventPriority.HIGH)
  public void extraPickaxe(BlockBreakEvent e){
    if(!getConfig().getBoolean("pickaxe-ores.enabled",false) || !e.isDropItems())return;
    Player p=e.getPlayer();
    if(p.getGameMode()!=GameMode.SURVIVAL)return;
    ItemStack tool=p.getInventory().getItemInMainHand();
    if(!isManaged(tool,8) || tool.getType()!=Material.NETHERITE_PICKAXE)return;
    if(!getConfig().getStringList("pickaxe-ores.allowed-blocks").contains(e.getBlock().getType().name()))return;
    double factor=Math.max(1.0,Math.min(8.0,getConfig().getDouble("pickaxe-ores.levels."+level(p,8)+".multiplier",1.0)));
    if(factor<=1.0)return;
    for(ItemStack drop:e.getBlock().getDrops(tool,p)){
      if(drop==null||drop.getType().isAir()||drop.getAmount()<=0)continue;
      double desired=drop.getAmount()*(factor-1.0);
      int extra=(int)Math.floor(desired);
      if(java.util.concurrent.ThreadLocalRandom.current().nextDouble()<desired-extra)extra++;
      while(extra>0){ItemStack bonus=drop.clone();int n=Math.min(extra,bonus.getMaxStackSize());bonus.setAmount(n);e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(),bonus);extra-=n;}
    }
  }
  // Only multiply ordinary drops from explicitly permitted shearable blocks.
  // Uses the actual held SmurfUpgrades shears and ignores creative mode.
  @EventHandler(ignoreCancelled=true, priority=EventPriority.HIGH)
  public void extraShears(BlockBreakEvent e){
    if(!getConfig().getBoolean("shears-drops.enabled",true) || !e.isDropItems())return;
    Player p=e.getPlayer();
    if(p.getGameMode()!=GameMode.SURVIVAL)return;
    ItemStack tool=p.getInventory().getItemInMainHand();
    if(!isManaged(tool,6) || tool.getType()!=Material.SHEARS)return;
    Material block=e.getBlock().getType();
    if(!getConfig().getStringList("shears-drops.allowed-blocks").contains(block.name()))return;
    int l=level(p,6);
    double factor=Math.max(1.0,Math.min(16.0,getConfig().getDouble("shears-drops.levels."+l+".multiplier",1.0)));
    if(factor<=1.0)return;
    Collection<ItemStack> drops=e.getBlock().getDrops(tool,p);
    for(ItemStack drop:drops){
      if(drop==null||drop.getType().isAir()||drop.getAmount()<=0)continue;
      // Restrict bonuses to the block itself, avoiding unrelated loot and containers.
      if(drop.getType()!=block)continue;
      double desired=drop.getAmount()*(factor-1.0);
      int extra=(int)Math.floor(desired);
      if(java.util.concurrent.ThreadLocalRandom.current().nextDouble()<desired-extra)extra++;
      while(extra>0){
        ItemStack bonus=drop.clone();int count=Math.min(extra,bonus.getMaxStackSize());
        bonus.setAmount(count);e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(),bonus);extra-=count;
      }
    }
  }
  @EventHandler public void join(PlayerJoinEvent e){Player p=e.getPlayer();boolean fresh=!players.contains("players."+p.getUniqueId());init(p);}
  @EventHandler public void death(PlayerDeathEvent e){if(!getConfig().getBoolean("starter.auto-respawn-set",true))return;e.getDrops().removeIf(i->i.hasItemMeta()&&i.getItemMeta().getPersistentDataContainer().has(ownedKey,PersistentDataType.STRING));}
  @EventHandler public void respawn(PlayerRespawnEvent e){}
  @EventHandler public void close(InventoryCloseEvent e){UUID id=e.getPlayer().getUniqueId();if(menuInventories.get(id)==e.getInventory()){open.remove(id);menuInventories.remove(id);}}
  @EventHandler public void click(InventoryClickEvent e){if(!(e.getWhoClicked() instanceof Player p))return;Menu menu=open.get(p.getUniqueId());if(menu==null||e.getView().getTopInventory()!=menuInventories.get(p.getUniqueId()))return;if(e.getView().getTopInventory()!=e.getClickedInventory()){if(e.isShiftClick()||e.getClick()==ClickType.DOUBLE_CLICK)e.setCancelled(true);return;}e.setCancelled(true);int slot=e.getRawSlot();
    if(slot==guiSlot("back",49)){switch(menu.kind){case "upgrade","preview"->main(p);case "setpreview"->sets(p);default->p.closeInventory();}return;}
    if(menu.kind.equals("main")){for(int t=0;t<types.length;t++)if(guiSlot("category."+types[t],categorySlots[t])==slot){if(e.isRightClick())preview(p,t);else upgrade(p,t);return;}}
    if(menu.kind.equals("upgrade")){for(int chance:new int[]{25,50,100})if(slot==guiSlot("chance."+chance,chance==25?11:chance==50?13:15)){attempt(p,menu.type,chance);return;}}
    if(menu.kind.equals("sets")&&slot>=10&&slot<22)setPreview(p,slot-9);
  }
  @EventHandler public void quit(PlayerQuitEvent e){open.remove(e.getPlayer().getUniqueId());menuInventories.remove(e.getPlayer().getUniqueId());}
  private final Set<UUID> busy=new HashSet<>();
  private void attempt(Player p,int type,int chance){if(!busy.add(p.getUniqueId()))return;try{int current=level(p,type);if(current>=12){p.sendMessage(tr("&cMasz już maksymalny poziom!"));return;}if(economy==null && current>0){p.sendMessage(tr("&cBrak ekonomii Vault! Administrator musi zainstalować Vault i plugin ekonomii."));return;}
      int next=current+1;double cost=current==0?0:price(type,next)*multiplier(chance);if(cost>0 && !economy.has(p,cost)){p.sendMessage(tr("&cNie masz wystarczająco pieniędzy. Potrzebujesz "+money(cost)+" $"));return;}
      var transaction=cost>0?economy.withdrawPlayer(p,cost):null;if(transaction!=null && !transaction.transactionSuccess()){p.sendMessage(tr("&cNie udało się pobrać pieniędzy."));return;}
      if(current==0 || Math.random()*100<chance){players.set(path(p,type),next);savePlayers();p.sendMessage(tr("&a✔ Sukces! "+labels[type]+" → "+character(next)+" ("+next+")"));effect(p,true,next);
        // Replace only the currently worn/held managed item, if present; unlock persists regardless.
        replaceManaged(p,type,next);
      }else{p.sendMessage(tr("&c✘ Nieudane ulepszenie! Stracono "+money(cost)+" $, ale poziom pozostaje."));effect(p,false,current);}
      upgrade(p,type);
    }finally{busy.remove(p.getUniqueId());}}
  private void giveSingle(Player p,int type,ItemStack item){
    Map<Integer,ItemStack> rest=p.getInventory().addItem(item);
    for(ItemStack stack:rest.values())p.getWorld().dropItemNaturally(p.getLocation(),stack);
    p.updateInventory();
  }
  // Replace all owned copies, including armor, hotbar and offhand. This also fixes stale ⭐1 items.
  private void replaceManaged(Player p,int type,int l){
    ItemStack updated=equipment(type,l,true);
    boolean found=false;
    PlayerInventory inv=p.getInventory();
    for(int slot=0;slot<inv.getSize();slot++){
      ItemStack old=inv.getItem(slot);
      if(isManaged(old,type)){
        inv.setItem(slot,updated.clone());
        found=true;
      }
    }
    if(isManaged(inv.getItemInOffHand(),type)){
      inv.setItemInOffHand(updated.clone());found=true;
    }
    if(!found)giveSingle(p,type,updated);
    p.updateInventory();
  }
  private boolean isAnyManaged(ItemStack i){return i!=null&&i.hasItemMeta()&&i.getItemMeta().getPersistentDataContainer().has(ownedKey,PersistentDataType.STRING);}
  private boolean isManaged(ItemStack i,int type){return i!=null&&i.hasItemMeta()&&types[type].equals(i.getItemMeta().getPersistentDataContainer().get(ownedKey,PersistentDataType.STRING));}
  @Override public boolean onCommand(CommandSender sender,Command cmd,String alias,String[] args){String name=cmd.getName();if(name.equals("smurfadmin")||name.equals("avengersadmin")){if(!sender.hasPermission("avengers.admin")&&!sender.hasPermission("smurf.admin")){sender.sendMessage(tr("&cBrak uprawnień."));return true;}if(args.length==1&&args[0].equalsIgnoreCase("reload")){loadOther();sender.sendMessage(tr("&aKonfiguracja przeładowana."));}else sender.sendMessage("/avengersadmin reload");return true;}
    if(!(sender instanceof Player p)){sender.sendMessage("Komenda tylko dla graczy.");return true;}init(p);switch(name){case "ulepsz"->main(p);case "cenyulepszen"->costs(p);case "topulepszen"->ranking(p);case "sety"->{if(args.length>0){try{int l=Integer.parseInt(args[0]);if(l>=1&&l<=12)setPreview(p,l);else p.sendMessage(tr("&cPoziom 1–12."));}catch(NumberFormatException ex){p.sendMessage(tr("&cPodaj poziom 1–12."));}}else sets(p);}}return true;}
}
