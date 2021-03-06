/**
 *                            License
 * THE WORK (AS DEFINED BELOW) IS PROVIDED UNDER THE TERMS OF THIS  
 * CREATIVE COMMONS PUBLIC LICENSE ("CCPL" OR "LICENSE"). 
 * THE WORK IS PROTECTED BY COPYRIGHT AND/OR OTHER APPLICABLE LAW.  
 * ANY USE OF THE WORK OTHER THAN AS AUTHORIZED UNDER THIS LICENSE OR  
 * COPYRIGHT LAW IS PROHIBITED.
 * 
 * BY EXERCISING ANY RIGHTS TO THE WORK PROVIDED HERE, YOU ACCEPT AND  
 * AGREE TO BE BOUND BY THE TERMS OF THIS LICENSE. TO THE EXTENT THIS LICENSE  
 * MAY BE CONSIDERED TO BE A CONTRACT, THE LICENSOR GRANTS YOU THE RIGHTS CONTAINED 
 * HERE IN CONSIDERATION OF YOUR ACCEPTANCE OF SUCH TERMS AND CONDITIONS.
 * 
 */
package l1j.server.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.logging.Logger;

import l1j.server.Config;
import l1j.server.L1Message;
import l1j.server.console.ConsoleProcess;
import l1j.server.server.datatables.CastleTable;
import l1j.server.server.datatables.CharacterTable;
import l1j.server.server.datatables.ChatLogTable;
import l1j.server.server.datatables.ClanTable;
import l1j.server.server.datatables.DoorTable;
import l1j.server.server.datatables.DropTable;
import l1j.server.server.datatables.DropItemTable;
import l1j.server.server.datatables.FurnitureItemTable;
import l1j.server.server.datatables.FurnitureSpawnTable;
import l1j.server.server.datatables.GetBackRestartTable;
import l1j.server.server.datatables.InnTable;
import l1j.server.server.datatables.IpTable;
import l1j.server.server.datatables.ItemTable;
import l1j.server.server.datatables.MagicDollTable;
import l1j.server.server.datatables.MailTable;
import l1j.server.server.datatables.MapsTable;
import l1j.server.server.datatables.MobGroupTable;
import l1j.server.server.datatables.NpcActionTable;
import l1j.server.server.datatables.NpcChatTable;
import l1j.server.server.datatables.NpcSpawnTable;
import l1j.server.server.datatables.NpcTable;
import l1j.server.server.datatables.NPCTalkDataTable;
import l1j.server.server.datatables.PetTable;
import l1j.server.server.datatables.PetTypeTable;
import l1j.server.server.datatables.PolyTable;
import l1j.server.server.datatables.RaceTicketTable;
import l1j.server.server.datatables.ResolventTable;
import l1j.server.server.datatables.ShopTable;
import l1j.server.server.datatables.SkillsTable;
import l1j.server.server.datatables.SpawnTable;
import l1j.server.server.datatables.SprTable;
import l1j.server.server.datatables.UBSpawnTable;
import l1j.server.server.datatables.WeaponSkillTable;
import l1j.server.server.model.Dungeon;
import l1j.server.server.model.ElementalStoneGenerator;
import l1j.server.server.model.Getback;
import l1j.server.server.model.L1BossCycle;
import l1j.server.server.model.L1CastleLocation;
import l1j.server.server.model.L1DeleteItemOnGround;
import l1j.server.server.model.L1NpcRegenerationTimer;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.game.L1BugBearRace;
import l1j.server.server.model.gametime.L1GameTimeClock;
import l1j.server.server.model.item.L1TreasureBox;
import l1j.server.server.model.map.L1WorldMap;
import l1j.server.server.model.npc.action.L1NpcDefaultAction;
import l1j.server.server.model.trap.L1WorldTraps;
import l1j.server.server.storage.mysql.MysqlAutoBackup;
import l1j.server.server.utils.MysqlAutoBackupTimer;
import l1j.server.server.utils.SystemUtil;

// Referenced classes of package l1j.server.server:
// ClientThread, Logins, RateTable, IdFactory,
// LoginController, GameTimeController, Announcements,
// MobTable, SpawnTable, SkillsTable, PolyTable,
// TeleportLocations, ShopTable, NPCTalkDataTable, NpcSpawnTable,
// IpTable, Shutdown, NpcTable, MobGroupTable, NpcShoutTable

public class GameServer extends Thread {
	private static Logger _log = Logger.getLogger(GameServer.class.getName());
	private static int YesNoCount = 0;
	public final int startTime = (int) (System.currentTimeMillis() / 1000);
	private ServerSocket _serverSocket;
	private int _port;
	private LoginController _loginController;
	private int chatlvl;

	@Override
	public void run() {
		System.out.println(L1Message.memoryUse + SystemUtil.getUsedMemoryMB() + L1Message.memory);
		System.out.println(L1Message.waitingforuser);
		while (true) {
			try {
				Socket socket = _serverSocket.accept();
				System.out.println(L1Message.from + socket.getInetAddress()+ L1Message.attempt);
				String host = socket.getInetAddress().getHostAddress();
				if (IpTable.getInstance().isBannedIp(host)) {
					_log.info("banned IP(" + host + ")");
				} else {
					ClientThread client = new ClientThread(socket);
					GeneralThreadPool.getInstance().execute(client);
				}
			} catch (IOException ioexception) {
			}
		}
	}

	private static GameServer _instance;

	private GameServer() {
		super("GameServer");
	}

	public static GameServer getInstance() {
		if (_instance == null) {
			_instance = new GameServer();
		}
		return _instance;
	}

	public void initialize() throws Exception {
		String s = Config.GAME_SERVER_HOST_NAME;
		double rateXp = Config.RATE_XP;
		double LA = Config.RATE_LA;
		double rateKarma = Config.RATE_KARMA;
		double rateDropItems = Config.RATE_DROP_ITEMS;
		double rateDropAdena = Config.RATE_DROP_ADENA;

		// Locale ????????????
		L1Message.getInstance();

		chatlvl = Config.GLOBAL_CHAT_LEVEL;
		_port = Config.GAME_SERVER_PORT;
		if (!"*".equals(s)) {
			InetAddress inetaddress = InetAddress.getByName(s);
			inetaddress.getHostAddress();
			_serverSocket = new ServerSocket(_port, 50, inetaddress);
			System.out.println(L1Message.setporton + _port);
		} else {
			_serverSocket = new ServerSocket(_port);
			System.out.println(L1Message.setporton + _port);
		}

		System.out.println("???????????????????????????????????????????????????????????????????????????????????????????????????");
		System.out.println("???     " + L1Message.ver + "\t" + "\t" + "???");
		System.out.println("???????????????????????????????????????????????????????????????????????????????????????????????????" + "\n");

		System.out.println(L1Message.settingslist + "\n");
		System.out.println("???" + L1Message.exp + ": " + (rateXp) + L1Message.x
				+ "\n\r???" + L1Message.justice + ": " + (LA) + L1Message.x
				+ "\n\r???" + L1Message.karma + ": " + (rateKarma) + L1Message.x
				+ "\n\r???" + L1Message.dropitems + ": " + (rateDropItems)+ L1Message.x 
				+ "\n\r???" + L1Message.dropadena + ": "+ (rateDropAdena) + L1Message.x 
				+ "\n\r???"+ L1Message.enchantweapon + ": "+ (Config.ENCHANT_CHANCE_WEAPON) + "%" 
				+ "\n\r???"+ L1Message.enchantarmor + ": " + (Config.ENCHANT_CHANCE_ARMOR)+ "%");
		System.out.println("???" + L1Message.chatlevel + ": " + (chatlvl)+ L1Message.level);

		if (Config.ALT_NONPVP) { // Non-PvP??????
			System.out.println("???" + L1Message.nonpvpNo + "\n");
		} else {
			System.out.println("???" + L1Message.nonpvpYes + "\n");
		}

		int maxOnlineUsers = Config.MAX_ONLINE_USERS;
		System.out.println(L1Message.maxplayer + (maxOnlineUsers)
				+ L1Message.player);

		System.out.println("???????????????????????????????????????????????????????????????????????????????????????????????????");
		System.out.println("???     " + L1Message.ver + "\t" + "\t" + "???");
		System.out.println("???????????????????????????????????????????????????????????????????????????????????????????????????" + "\n");

		IdFactory.getInstance();
		L1WorldMap.getInstance();
		_loginController = LoginController.getInstance();
		_loginController.setMaxAllowedOnlinePlayers(maxOnlineUsers);

		// ????????????????????????
		CharacterTable.getInstance().loadAllCharName();

		// ??????????????????????????????
		CharacterTable.clearOnlineStatus();

		// ?????????????????????
		L1GameTimeClock.init();

		// ?????????????????????
		UbTimeController ubTimeContoroller = UbTimeController.getInstance();
		GeneralThreadPool.getInstance().execute(ubTimeContoroller);
		
		// ???????????????
		WarTimeController warTimeController = WarTimeController.getInstance();
		GeneralThreadPool.getInstance().execute(warTimeController);
		
		// ????????????????????????
		if (Config.ELEMENTAL_STONE_AMOUNT > 0) {
			ElementalStoneGenerator elementalStoneGenerator = ElementalStoneGenerator.getInstance();
			GeneralThreadPool.getInstance().execute(elementalStoneGenerator);
		}

		// ????????? HomeTown ??????
		HomeTownTimeController.getInstance();

		// ?????????????????????
		AuctionTimeController auctionTimeController = AuctionTimeController.getInstance();
		GeneralThreadPool.getInstance().execute(auctionTimeController);

		// ????????????????????????
		HouseTaxTimeController houseTaxTimeController = HouseTaxTimeController.getInstance();
		GeneralThreadPool.getInstance().execute(houseTaxTimeController);

		// ???????????????
		FishingTimeController fishingTimeController = FishingTimeController.getInstance();
		GeneralThreadPool.getInstance().execute(fishingTimeController);

		// ????????? NPC ??????
		NpcChatTimeController npcChatTimeController = NpcChatTimeController.getInstance();
		GeneralThreadPool.getInstance().execute(npcChatTimeController);

		// ????????? Light
		LightTimeController lightTimeController = LightTimeController.getInstance();
		GeneralThreadPool.getInstance().execute(lightTimeController);

		// ?????????????????????
		Announcements.getInstance();
		
		// ???????????????????????????
	    AnnouncementsCycle.getInstance();

		// ?????????MySQL??????????????????
		MysqlAutoBackup.getInstance();

		// ?????? MySQL?????????????????? ?????????
		MysqlAutoBackupTimer.TimerStart();
		
		// ???????????????????????????
		Account.InitialOnlineStatus();

		NpcTable.getInstance();
		L1DeleteItemOnGround deleteitem = new L1DeleteItemOnGround();
		deleteitem.initialize();

		if (!NpcTable.getInstance().isInitialized()) {
			throw new Exception("Could not initialize the npc table");
		}
		L1NpcDefaultAction.getInstance();
		DoorTable.initialize();
		SpawnTable.getInstance();
		MobGroupTable.getInstance();
		SkillsTable.getInstance();
		PolyTable.getInstance();
		ItemTable.getInstance();
		DropTable.getInstance();
		DropItemTable.getInstance();
		ShopTable.getInstance();
		NPCTalkDataTable.getInstance();
		L1World.getInstance();
		L1WorldTraps.getInstance();
		Dungeon.getInstance();
		NpcSpawnTable.getInstance();
		IpTable.getInstance();
		MapsTable.getInstance();
		UBSpawnTable.getInstance();
		PetTable.getInstance();
		ClanTable.getInstance();
		CastleTable.getInstance();
		L1CastleLocation.setCastleTaxRate(); // ????????? CastleTable ???????????????
		GetBackRestartTable.getInstance();
		GeneralThreadPool.getInstance();
		L1NpcRegenerationTimer.getInstance();
		ChatLogTable.getInstance();
		WeaponSkillTable.getInstance();
		NpcActionTable.load();
		GMCommandsConfig.load();
		Getback.loadGetBack();
		PetTypeTable.load();
		L1BossCycle.load();
		L1TreasureBox.load();
		SprTable.getInstance();
		ResolventTable.getInstance();
		FurnitureSpawnTable.getInstance();
		NpcChatTable.getInstance();
		MailTable.getInstance();
		RaceTicketTable.getInstance();
		L1BugBearRace.getInstance();
		InnTable.getInstance();
		MagicDollTable.getInstance();
		FurnitureItemTable.getInstance();

		System.out.println(L1Message.initialfinished);
		Runtime.getRuntime().addShutdownHook(Shutdown.getInstance());
		
		// cmd????????????
		Thread cp = new ConsoleProcess();
		cp.start();
		
		this.start();
	}

	/**
	 * ??????????????????????????????????????????????????????
	 */
	public void disconnectAllCharacters() {
		Collection<L1PcInstance> players = L1World.getInstance()
				.getAllPlayers();
		for (L1PcInstance pc : players) {
			pc.getNetConnection().setActiveChar(null);
			pc.getNetConnection().kick();
		}
		// ??????????????????????????????
		for (L1PcInstance pc : players) {
			ClientThread.quitGame(pc);
			L1World.getInstance().removeObject(pc);
			Account account = Account.load(pc.getAccountName());
			Account.online(account, false);
		}
	}

	private class ServerShutdownThread extends Thread {
		private final int _secondsCount;

		public ServerShutdownThread(int secondsCount) {
			_secondsCount = secondsCount;
		}

		@Override
		public void run() {
			L1World world = L1World.getInstance();
			try {
				int secondsCount = _secondsCount;
				world.broadcastServerMessage("????????????????????????");
				world.broadcastServerMessage("??????????????????????????????????????????");
				while (0 < secondsCount) {
					if (secondsCount <= 30) {
						world.broadcastServerMessage("???????????????" + secondsCount
								+ "????????????????????????????????????????????????????????????");
					} else {
						if (secondsCount % 60 == 0) {
							world.broadcastServerMessage("???????????????" + secondsCount
									/ 60 + "??????????????????");
						}
					}
					Thread.sleep(1000);
					secondsCount--;
				}
				shutdown();
			} catch (InterruptedException e) {
				world.broadcastServerMessage("?????????????????????????????????????????????????????????");
				return;
			}
		}
	}

	private ServerShutdownThread _shutdownThread = null;

	public synchronized void shutdownWithCountdown(int secondsCount) {
		if (_shutdownThread != null) {
			// ??????????????????
			// TODO ?????????????????????????????????
			return;
		}
		_shutdownThread = new ServerShutdownThread(secondsCount);
		GeneralThreadPool.getInstance().execute(_shutdownThread);
	}

	public void shutdown() {
		disconnectAllCharacters();
		System.exit(0);
	}

	public synchronized void abortShutdown() {
		if (_shutdownThread == null) {
			// ??????????????????
			// TODO ?????????????????????????????????
			return;
		}

		_shutdownThread.interrupt();
		_shutdownThread = null;
	}

	/**
	 * ?????????????????????YesNo?????????
	 * @return YesNo?????????
	 */
	public static int getYesNoCount() {
		YesNoCount += 1;
		return YesNoCount;
	}
}
