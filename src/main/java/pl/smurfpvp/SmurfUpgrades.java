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
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

public final class SmurfUpgrades extends JavaPlugin implements Listener, CommandExecutor {
  private final String[] types={"helmet","chestplate","leggings","boots","sword","axe","shears","elytra"};
  private final String[] labels={"Główka","Klata","Spodnie","Buty","Miecz","Siekiera","Nożyce","Elytra"};
  private final Material[] materials={Material.PLAYER_HEAD,Material.LEATHER_CHESTPLATE,Material.LEATHER_LEGGINGS,Material.LEATHER_BOOTS,Material.DIAMOND_SWORD,Material.DIAMOND_AXE,Material.SHEARS,Material.ELYTRA};
  private final int[] categorySlots={10,11,12,13,14,15,16,22};
  private Economy economy; private FileConfiguration upgrades,gui,players;
  private File playersFile; private final Map<UUID,Menu> open=new HashMap<>(); private final Map<UUID,Inventory> menuInventories=new HashMap<>();
  private record Menu(String kind,int type,int level) {}
  private NamespacedKey ownedKey;

  @Override public void onEnable(){saveDefaultConfig(); ensure("upgrades.yml");ensure("gui.yml");playersFile=new File(getDataFolder(),"players.yml");players=YamlConfiguration.loadConfiguration(playersFile);loadOther();ownedKey=new NamespacedKey(this,"smurf_item");
    if(getServer().getPluginManager().getPlugin("Vault")!=null){RegisteredServiceProvider<Economy> p=getServer().getServicesManager().getRegistration(Economy.class);if(p!=null)economy=p.getProvider();}
    for(String c:new String[]{"ulepsz","sety","cenyulepszen","smurfadmin"}) Objects.requireNonNull(getCommand(c)).setExecutor(this);
    getServer().getPluginManager().registerEvents(this,this);
    getLogger().info("SmurfUpgrades enabled; Vault economy: "+(economy!=null));
  }
  private void ensure(String name){File f=new File(getDataFolder(),name);if(!f.exists())saveResource(name,false);}
  private void loadOther(){reloadConfig();upgrades=YamlConfiguration.loadConfiguration(new File(getDataFolder(),"upgrades.yml"));gui=YamlConfiguration.loadConfiguration(new File(getDataFolder(),"gui.yml"));}
  private void savePlayers(){try{players.save(playersFile);}catch(Exception e){getLogger().severe("Cannot save players.yml: "+e.getMessage());}}
  private String path(Player p,int type){return "players."+p.getUniqueId()+"."+types[type];}
  private int level(Player p,int type){return Math.max(1,Math.min(12,players.getInt(path(p,type),1)));}
  private void init(Player p){if(players.contains("players."+p.getUniqueId()))return;for(int i=0;i<8;i++)players.set(path(p,i),1);savePlayers();}
  private String tr(String s){return ChatColor.translateAlternateColorCodes('&',s==null?"":s);}
  private String title(String name){return tr("&8🍄 "+name);}
  private String character(int l){return upgrades.getString("levels."+l+".name","Poziom "+l);}
  private double price(int type,int l){return Math.max(0,upgrades.getDouble("items."+types[type]+".levels."+l+".price",upgrades.getDouble("levels."+l+".price",0)));}
  private double multiplier(int chance){return getConfig().getDouble("chances."+chance+".price-multiplier",chance/100.0);}
  private ItemStack simple(Material m,String name,String... lore){ItemStack i=new ItemStack(m);ItemMeta meta=i.getItemMeta();meta.setDisplayName(tr(name));meta.setLore(Arrays.stream(lore).map(this::tr).toList());i.setItemMeta(meta);return i;}
  private int guiSlot(String key,int fallback){return gui.getInt("slots."+key,fallback);}
  private void put(Inventory inv,int slot,ItemStack item){if(slot>=0&&slot<inv.getSize())inv.setItem(slot,item);}
  private Inventory inventory(String name){Inventory inv=Bukkit.createInventory(null,54,title(name));Material glass=Material.matchMaterial(gui.getString("background","GRAY_STAINED_GLASS_PANE"));if(glass==null)glass=Material.GRAY_STAINED_GLASS_PANE;ItemStack filler=simple(glass," ");for(int s=0;s<54;s++)inv.setItem(s,filler);return inv;}
  private void display(Player p,Inventory inv,String kind,int type,int l){open.put(p.getUniqueId(),new Menu(kind,type,l));menuInventories.put(p.getUniqueId(),inv);p.openInventory(inv);}
  private void main(Player p){Inventory inv=inventory("ULEPSZENIA");for(int t=0;t<8;t++)put(inv,guiSlot("category."+types[t],categorySlots[t]),simple(materials[t],"&b"+labels[t],"&7LPM: ulepszaj","&7PPM: zobacz wszystkie poziomy","&fTwój poziom: &e"+level(p,t)));put(inv,guiSlot("back",49),simple(Material.BARRIER,"&cZamknij"));display(p,inv,"main",-1,0);}
  private void upgrade(Player p,int type){int current=level(p,type);Inventory inv=inventory("ULEPSZ: "+labels[type]);
    for(int chance:new int[]{25,50,100}){int slot=guiSlot("chance."+chance,chance==25?11:chance==50?13:15);int next=Math.min(12,current+1);double cost=price(type,next)*multiplier(chance);put(inv,slot,simple(chance==25?Material.ORANGE_CONCRETE:chance==50?Material.YELLOW_CONCRETE:Material.LIME_CONCRETE,"&e"+chance+"% szansy","&7Cena próby: &6"+money(cost)+" $","&7Kliknij, aby spróbować"));}
    put(inv,guiSlot("current",21),equipment(type,current,false));put(inv,guiSlot("next",23),current==12?simple(Material.NETHER_STAR,"&dMAX POZIOM"):equipment(type,current+1,false));put(inv,guiSlot("back",49),simple(Material.ARROW,"&cWróć"));display(p,inv,"upgrade",type,current);}
  private String money(double d){return String.format(Locale.US,"%,.0f",d);}
  private void preview(Player p,int type){Inventory inv=inventory("POZIOMY: "+labels[type]);for(int l=1;l<=12;l++)put(inv,10+l-1,equipment(type,l,false));put(inv,guiSlot("back",49),simple(Material.ARROW,"&cWróć"));display(p,inv,"preview",type,0);}
  private void sets(Player p){Inventory inv=inventory("WSZYSTKIE SETY");for(int l=1;l<=12;l++)put(inv,10+l-1,head(l,"&b"+l+". "+character(l),"&7Kliknij: pełny podgląd","&7Koszt tego poziomu: &6"+money(total(l))+" $"));put(inv,guiSlot("back",49),simple(Material.BARRIER,"&cZamknij"));display(p,inv,"sets",-1,0);}
  private double total(int l){double sum=0;for(int t=0;t<8;t++)sum+=price(t,l);return sum;}
  private void setPreview(Player p,int l){Inventory inv=inventory("SET "+l+": "+character(l));for(int t=0;t<8;t++)put(inv,categorySlots[t],equipment(t,l,false));put(inv,31,simple(Material.GOLD_INGOT,"&6Cena pełnego poziomu: "+money(total(l))+" $","&7Łączny koszt od I do "+l+": &e"+money(cumulative(l))+" $"));put(inv,guiSlot("back",49),simple(Material.ARROW,"&cWróć"));display(p,inv,"setpreview",-1,l);}
  private double cumulative(int l){double sum=0;for(int j=2;j<=l;j++)sum+=total(j);return sum;}
  private void costs(Player p){Inventory inv=inventory("CENNIK SETÓW");for(int l=1;l<=12;l++)put(inv,10+l-1,simple(Material.GOLD_INGOT,"&e"+l+". "+character(l),"&7Cena przejścia wszystkich części: &6"+money(total(l))+" $","&7Łącznie od poziomu 1: &6"+money(cumulative(l))+" $"));put(inv,guiSlot("back",49),simple(Material.BARRIER,"&cZamknij"));display(p,inv,"costs",-1,0);}
  private ItemStack head(int l,String name,String... lore){String id=upgrades.getString("levels."+l+".head-id","");ItemStack item=null;
    if(!id.isBlank()&&Bukkit.getPluginManager().isPluginEnabled("HeadDatabase")){try{Class<?> c=Class.forName("me.arcaniax.hdb.api.HeadDatabaseAPI");Object api=c.getDeclaredConstructor().newInstance();Method m=c.getMethod("getItemHead",String.class);Object got=m.invoke(api,id);if(got instanceof ItemStack stack&&stack.getType()==Material.PLAYER_HEAD)item=stack.clone();}catch(Exception e){getLogger().warning("HeadDatabase ID "+id+" unavailable: "+e.getMessage());}}
    if(item==null){item=new ItemStack(Material.PLAYER_HEAD);if(id.isBlank())getLogger().fine("No HeadDatabase ID for level "+l);}ItemMeta meta=item.getItemMeta();meta.setDisplayName(tr(name));meta.setLore(Arrays.stream(lore).map(this::tr).toList());item.setItemMeta(meta);return item;}
  private ItemStack equipment(int type,int l,boolean tagged){ItemStack item=type==0?head(l,"&b"+character(l)+" &7- Główka"):new ItemStack(materials[type]);ItemMeta meta=item.getItemMeta();meta.setDisplayName(tr("&b"+labels[type]+" &8» &e"+character(l)+" &7["+l+"]"));
    if(meta instanceof LeatherArmorMeta leather){try{leather.setColor(Color.fromRGB(Integer.parseInt(upgrades.getString("levels."+l+".color","#FFFFFF").replace("#",""),16)));}catch(Exception ignored){}}
    String enchPath="items."+types[type]+".levels."+l+".enchants";var section=upgrades.getConfigurationSection(enchPath);if(section!=null)for(String key:section.getKeys(false)){Enchantment ench=Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key.toLowerCase(Locale.ROOT)));if(ench!=null){int value=Math.max(1,Math.min(32767,section.getInt(key)));meta.addEnchant(ench,value,true);}}
    meta.setLore(List.of(tr("&7Poziom: &e"+l+" &8/ &e12"),tr("&7Postać: &b"+character(l)),tr("&7Cena przejścia: &6"+money(price(type,l))+" $")));
    if(tagged)meta.getPersistentDataContainer().set(ownedKey,PersistentDataType.STRING,types[type]);item.setItemMeta(meta);return item;}
  private void give(Player p){for(int slot=0;slot<p.getInventory().getSize();slot++){ItemStack old=p.getInventory().getItem(slot);if(isAnyManaged(old))p.getInventory().setItem(slot,null);}for(int t=0;t<8;t++){ItemStack item=equipment(t,level(p,t),true);if(t<4){switch(t){case 0->p.getInventory().setHelmet(item);case 1->p.getInventory().setChestplate(item);case 2->p.getInventory().setLeggings(item);case 3->p.getInventory().setBoots(item);}}else p.getInventory().addItem(item);}}
  @EventHandler public void join(PlayerJoinEvent e){Player p=e.getPlayer();boolean fresh=!players.contains("players."+p.getUniqueId());init(p);if(fresh&&getConfig().getBoolean("starter.give-on-first-join",true))Bukkit.getScheduler().runTaskLater(this,()->give(p),10L);}
  @EventHandler public void death(PlayerDeathEvent e){if(!getConfig().getBoolean("starter.auto-respawn-set",true))return;e.getDrops().removeIf(i->i.hasItemMeta()&&i.getItemMeta().getPersistentDataContainer().has(ownedKey,PersistentDataType.STRING));}
  @EventHandler public void respawn(PlayerRespawnEvent e){if(getConfig().getBoolean("starter.auto-respawn-set",true))Bukkit.getScheduler().runTaskLater(this,()->give(e.getPlayer()),2L);}
  @EventHandler public void close(InventoryCloseEvent e){UUID id=e.getPlayer().getUniqueId();if(menuInventories.get(id)==e.getInventory()){open.remove(id);menuInventories.remove(id);}}
  @EventHandler public void click(InventoryClickEvent e){if(!(e.getWhoClicked() instanceof Player p))return;Menu menu=open.get(p.getUniqueId());if(menu==null||e.getView().getTopInventory()!=menuInventories.get(p.getUniqueId()))return;if(e.getView().getTopInventory()!=e.getClickedInventory()){if(e.getClickedInventory()==p.getInventory()&&e.isShiftClick())e.setCancelled(true);return;}e.setCancelled(true);int slot=e.getRawSlot();
    if(slot==guiSlot("back",49)){switch(menu.kind){case "upgrade","preview"->main(p);case "setpreview"->sets(p);default->p.closeInventory();}return;}
    if(menu.kind.equals("main")){for(int t=0;t<8;t++)if(guiSlot("category."+types[t],categorySlots[t])==slot){if(e.isRightClick())preview(p,t);else upgrade(p,t);return;}}
    if(menu.kind.equals("upgrade")){for(int chance:new int[]{25,50,100})if(slot==guiSlot("chance."+chance,chance==25?11:chance==50?13:15)){attempt(p,menu.type,chance);return;}}
    if(menu.kind.equals("sets")&&slot>=10&&slot<22)setPreview(p,slot-9);
  }
  @EventHandler public void quit(PlayerQuitEvent e){open.remove(e.getPlayer().getUniqueId());menuInventories.remove(e.getPlayer().getUniqueId());}
  private final Set<UUID> busy=new HashSet<>();
  private void attempt(Player p,int type,int chance){if(!busy.add(p.getUniqueId()))return;try{int current=level(p,type);if(current>=12){p.sendMessage(tr("&cMasz już maksymalny poziom!"));return;}if(economy==null){p.sendMessage(tr("&cBrak ekonomii Vault! Administrator musi zainstalować Vault i plugin ekonomii."));return;}
      int next=current+1;double cost=price(type,next)*multiplier(chance);if(!economy.has(p,cost)){p.sendMessage(tr("&cNie masz wystarczająco pieniędzy. Potrzebujesz "+money(cost)+" $"));return;}
      var transaction=economy.withdrawPlayer(p,cost);if(!transaction.transactionSuccess()){p.sendMessage(tr("&cNie udało się pobrać pieniędzy."));return;}
      if(Math.random()*100<chance){players.set(path(p,type),next);savePlayers();p.sendMessage(tr("&a✔ Sukces! "+labels[type]+" → "+character(next)+" ("+next+")"));p.playSound(p.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,1,1.2f);
        // Replace only the currently worn/held managed item, if present; unlock persists regardless.
        replaceManaged(p,type,next);
      }else{p.sendMessage(tr("&c✘ Nieudane ulepszenie! Stracono "+money(cost)+" $, ale poziom pozostaje."));p.playSound(p.getLocation(),Sound.ENTITY_VILLAGER_NO,1,1);}
      upgrade(p,type);
    }finally{busy.remove(p.getUniqueId());}}
  private void replaceManaged(Player p,int type,int l){ItemStack updated=equipment(type,l,true);if(type==0){if(isManaged(p.getInventory().getHelmet(),type))p.getInventory().setHelmet(updated);}else if(type==1){if(isManaged(p.getInventory().getChestplate(),type))p.getInventory().setChestplate(updated);}else if(type==2){if(isManaged(p.getInventory().getLeggings(),type))p.getInventory().setLeggings(updated);}else if(type==3){if(isManaged(p.getInventory().getBoots(),type))p.getInventory().setBoots(updated);}else{for(int slot=0;slot<p.getInventory().getSize();slot++){if(isManaged(p.getInventory().getItem(slot),type)){p.getInventory().setItem(slot,updated);break;}}}}
  private boolean isAnyManaged(ItemStack i){return i!=null&&i.hasItemMeta()&&i.getItemMeta().getPersistentDataContainer().has(ownedKey,PersistentDataType.STRING);}
  private boolean isManaged(ItemStack i,int type){return i!=null&&i.hasItemMeta()&&types[type].equals(i.getItemMeta().getPersistentDataContainer().get(ownedKey,PersistentDataType.STRING));}
  @Override public boolean onCommand(CommandSender sender,Command cmd,String alias,String[] args){String name=cmd.getName();if(name.equals("smurfadmin")){if(!sender.hasPermission("smurf.admin")){sender.sendMessage(tr("&cBrak uprawnień."));return true;}if(args.length==1&&args[0].equalsIgnoreCase("reload")){loadOther();sender.sendMessage(tr("&aKonfiguracja przeładowana."));}else sender.sendMessage("/smurfadmin reload");return true;}
    if(!(sender instanceof Player p)){sender.sendMessage("Komenda tylko dla graczy.");return true;}init(p);switch(name){case "ulepsz"->main(p);case "cenyulepszen"->costs(p);case "sety"->{if(args.length>0){try{int l=Integer.parseInt(args[0]);if(l>=1&&l<=12)setPreview(p,l);else p.sendMessage(tr("&cPoziom 1–12."));}catch(NumberFormatException ex){p.sendMessage(tr("&cPodaj poziom 1–12."));}}else sets(p);}}return true;}
}
