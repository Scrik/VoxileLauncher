package ru.fourgotten.VoxileLauncher.Game;

import ru.fourgotten.VoxileSecurity.Data.Platform;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import ru.fourgotten.VoxileLauncher.Game.GameLaunchHelper.ServersDatEntry;
import ru.fourgotten.VoxileLauncher.Game.GameLaunchHelper.TextProperty;
import ru.fourgotten.VoxileLauncher.GlobalConfig;
import ru.fourgotten.VoxileLauncher.Launcher;
import ru.fourgotten.VoxileLauncher.Utilities;
import ru.fourgotten.VoxileSecurity.Data.MessagesWrapper.MessageWrappedGame;
import ru.fourgotten.VoxileSecurity.Data.OfflineClient;
import ru.fourgotten.VoxileSecurity.HashAndCipherUtilities;
import ru.fourgotten.VoxileSecurity.SecureConnection;
import ru.fourgotten.VoxileSecurity.SecureSocketWrapper;
import ru.fourgotten.VoxileSecurity.WrappedGameStarter;

public class GameLauncher extends WrappedGameStarter
{
	private final Launcher launcher;
	private ServerSocket serverSecure;
	public GameLauncher(Launcher launcher)
	{
		this.launcher = launcher;
	}
	public void checkAndRunClient(final OfflineClient client)
	{
		launcher.properties.data.lastStartedClient = client.caption;
		launcher.properties.saveToDisk();
		// Проверка файлов самого клиента
		Launcher.showGrant("Проверка целостности клиента...");
		new Thread()
		{
			@Override
			public void run()
			{
				// Обновление ресурсных файлов игры
				launcher.resources.checkClientAssets(client);
				// Проверка клиента
				boolean forceUpdate = false;
				try
				{
					final File clientJarFile = client.getClientJar();
					if(!clientJarFile.isFile())
					{
						Launcher.showError("Клиент не установлен");
						Utilities.sleep(2);
						forceUpdate = true;
					} else {
						final String script = GlobalConfig.urlScripts
							+ "client_md5.php?user=" + launcher.authentication.getPlayerName()
							+ "&client=" + client.caption
							+ "&hash=" + HashAndCipherUtilities.fileToMD5(clientJarFile);
						final URL url = new URL(script);
						final String result = new BufferedReader(new InputStreamReader(url.openStream())).readLine();
						if("NO".equalsIgnoreCase(result))
						{
							Launcher.showError("Обнаружена модификация клиента");
							Utilities.sleep(3);
							forceUpdate = true;
						} else if(!"OK".equalsIgnoreCase(result)) {
							Launcher.showError("Не удалось проверить клиент: " + result);
							Utilities.sleep(2);
							forceUpdate = true;
						}
					}
				} catch(IOException ex) {
					Launcher.showError(ex.toString());
					return;
				}
				Launcher.showGrant("Подготовка к запуску игры...");
				client.clean();
				try
				{
					// Докачка старых файлов игры
					if(forceUpdate)
						launcher.resources.updateClientFiles(client);
				} catch(IOException ex) {
					Launcher.showError("Не удалось обновить клиент!");
					Utilities.sleep(2);
					return;
				}
				// Запуск игры
				Launcher.showGrant("Запуск игры...");
				launcher.launcherFrame.setTitle(GlobalConfig.createMainFrameCaption(false) + " :: " + client.captionLocalized);
				launcher.launcherFrame.setVisible(false);
				launcher.gameLauncher.launchClient(client);
				launcher.launcherFrame.gameFinished();
			}
		}.start();
	}
	public void preLaunchActions(OfflineClient client)
	{
		// Установка нужных адресов северов
		GameLaunchHelper.setServersDatTopAddresses(client, new ServersDatEntry[]
		{
			// new ServersDatEntry("§eЦентральное лобби voxile.ru",       "s0.voxile.ru",       true),
			new ServersDatEntry("§dОсновной игровой сервер 'Primary'", "s1.voxile.ru:25575", true),
			new ServersDatEntry("§2Резервный адрес сервера 'Primary'",  "methuselah.ru",      true),
		});
		// Фишки, которые пришли к нам извне! :D
		final TextProperty[] propsOptifine = new TextProperty[]
		{
			new GameLaunchHelper.TextPropertyColonSep("tweakerClass", "1.0"),
		};
		GameLaunchHelper.processTextFile(client, "optionsof.txt", propsOptifine);
	}
	public void launchClient(OfflineClient client)
	{
 		preLaunchActions(client);
		if(GlobalConfig.separateGameProcess)
		{
			final String clientFolder = client.getClientHome().getAbsolutePath() + File.separator;
			// Параметры JVM
			final ArrayList<String> cmdline = new ArrayList<>();
			cmdline.add((GlobalConfig.platform == Platform.WINDOWS) ? "javaw" : "java");
			// cmdline.add("-Xdebug");
			// cmdline.add("-Xrunjdwp:transport=dt_socket,address=25600,server=y");
			cmdline.add("-Xmx" + Integer.toString(launcher.properties.data.nMemoryAllocationMB) + "m");
			cmdline.add("-Xmn128M");
			cmdline.add("-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump");
			cmdline.add("-XX:+UseConcMarkSweepGC");
			cmdline.add("-XX:+CMSIncrementalMode");
			cmdline.add("-XX:-UseAdaptiveSizePolicy");
			cmdline.add("-Djava.library.path=" + clientFolder + "natives");
			if(client.additionalJavaArguments != null)
				cmdline.addAll(Arrays.asList(client.additionalJavaArguments.split("\\s")));
			cmdline.add("-cp");
			cmdline.add(clientFolder + client.jarFile);
			cmdline.add("ru.fourgotten.VoxileWrapper.Wrapper");
			cmdline.add("--port");
			cmdline.add(Integer.toString(startLocalSecureServer(client)));
			startChildProcess(client, cmdline);
			stopLocalSecureServer();
		} else {
			WrappedGameStarter.instance = this;
			final MessageWrappedGame wrapperMessage = createStartupWrapperMessage(client);
			wrapperMessage.tweakerClass = "ru.fourgotten.VoxileWrapper.Tweaker";
			startGameInCurrentProcess(wrapperMessage);
		}
	}
	private void startChildProcess(OfflineClient client, ArrayList<String> params)
	{
		try
		{
			final ProcessBuilder pb = new ProcessBuilder(params);
			pb.directory(client.getClientHome());
			pb.redirectErrorStream(true);
			final Process process = pb.start();
			process.getOutputStream().close();
			new Thread()
			{
				@Override
				public void run()
				{
					try
					{
						final BufferedReader bri = new BufferedReader(new InputStreamReader(process.getInputStream()));
						for(String line = bri.readLine(); line != null; line = bri.readLine())
							System.err.println(line);
					} catch(IOException ex) {
					}
				}
			}.start();
			new Thread()
			{
				@Override
				public void run()
				{
					try
					{
						final BufferedReader bre = new BufferedReader(new InputStreamReader(process.getErrorStream()));
						for(String line = bre.readLine(); line != null; line = bre.readLine())
							System.err.println(line);
					} catch(IOException ex) {
					}
				}
			}.start();
			process.waitFor();
			process.getInputStream().close();
			process.getErrorStream().close();
			if(process.exitValue() != 0)
				Launcher.showError("Игра аварийно закрылась (код " + process.exitValue() + ")");
			else
				Launcher.showGrant("Игра завершена");
		} catch(IOException | InterruptedException ex) {
		}
	}
	private MessageWrappedGame createStartupWrapperMessage(OfflineClient gameInfo)
	{
		final MessageWrappedGame result = launcher.authentication.createWrapperMessage();
		result.gameDir = gameInfo.getClientHome().getAbsolutePath();
		result.version = gameInfo.caption;
		result.assetsDir = launcher.resources.getGlobalAssetsDir();
		if(gameInfo.assetIndexFile != null)
			result.assetIndex = gameInfo.assetIndexFile.getName().replace(".json", "");
		result.nativesDir = gameInfo.getClientHome() + File.separator + "natives";
		final ArrayList<String> fullLibPaths = new ArrayList<>();
		fullLibPaths.add(result.gameDir + File.separator + gameInfo.jarFile);
		for(String library : gameInfo.libraries)
			fullLibPaths.add(result.gameDir + File.separator + library);
		result.libraries = fullLibPaths.toArray(new String[fullLibPaths.size()]);
		result.mainClass = gameInfo.mainClass;
		result.arguments = (gameInfo.additionalGameArguments != null)
			? gameInfo.additionalGameArguments.split("\\s")
			: new String[] {};
		return result;
	}
	private int startLocalSecureServer(final OfflineClient gameInfo)
	{
		stopLocalSecureServer();
		final MessageWrappedGame message = createStartupWrapperMessage(gameInfo);
		try
		{
			serverSecure = SSLServerSocketFactory.getDefault().createServerSocket();
			serverSecure.bind(null);
			final Thread dispatcherSecure = new Thread()
			{
				@Override
				public void run()
				{
					while(!Thread.interrupted())
					{
						try
						{
							final SSLSocket socket = (SSLSocket)serverSecure.accept();
							if(socket == null)
								break;
							SecureConnection.tryEnableCipherSuites(socket);
							try(SecureSocketWrapper wrapper = new SecureSocketWrapper(socket))
							{
								if("hello!".equalsIgnoreCase(wrapper.readLine()))
									wrapper.writeLine("greetings!");
								if("wrapper2launcher".equalsIgnoreCase(wrapper.readLine()))
									wrapper.writeObject(message, MessageWrappedGame.class);
							}
						} catch(IOException ex) {
						} catch(RuntimeException ex) {
							break;
						}
					}
				}
			};
			dispatcherSecure.start();
		} catch (IOException ex) {
		}
		return serverSecure.getLocalPort();
	}
	private void stopLocalSecureServer()
	{
		try
		{
			serverSecure.close();
		} catch(IOException | NullPointerException ex) {
		}
		serverSecure = null;
	}
}
