package ru.fourgotten.VoxileLauncher.GUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import ru.fourgotten.VoxileLauncher.GUI.Controls.TransparentLabel;
import static ru.fourgotten.VoxileLauncher.GUI.Designer.openLink;
import ru.fourgotten.VoxileSecurity.Data.OfflineProject;
import ru.fourgotten.VoxileLauncher.GlobalConfig;
import ru.fourgotten.VoxileLauncher.Launcher;
import ru.fourgotten.VoxileLauncher.Utilities;
import ru.fourgotten.VoxileSecurity.Data.MessagesLauncher.AnswerLauncherDesign;

public final class FrameLauncherMain extends Designer
{
	private final InterfaceActions actionExecutor = new InterfaceActions(this);
	protected final TransparentLabel lnkSwitchToOtherProject =
		new TransparentLabel("<html><b><u>Переключиться на другой проект</u></b></html>");
	public FrameLauncherMain(Launcher launcher, OfflineProject project, AnswerLauncherDesign designDesc)
	{
		super(launcher, project, designDesc);
		setFavicon("favicon.png");
		setBackground();
		setLayout(new BorderLayout(0, 0));
		setResizable(false);
		setPreferredSize(new Dimension(854, 480));
		// Построение дочерних панелей
		superPanelLogin.setPreferredSize(new Dimension(435, 180));
		superPanelLogin.setBackground(new Color(255, 255, 239, 192));
		superPanelLogin.add(panelLogin);
		superPanelLogin.setVisible(false);
		superPanelClients.setPreferredSize(new Dimension(435, 180));
		superPanelClients.setBackground(new Color(255, 255, 239, 192));
		superPanelClients.add(panelClients);
		superPanelClients.setVisible(false);
		superPanelOptions.setPreferredSize(new Dimension(435, 180));
		superPanelOptions.setBackground(new Color(192, 255, 192, 127));
		superPanelOptions.add(panelOptions);
		superPanelOptions.setVisible(false);
		superPanelLinks.setPreferredSize(new Dimension(435, 180));
		superPanelLinks.setBackground(new Color(0, 0, 0, 127));
		superPanelLinks.add(panelLinks);
		superPanelLinks.setVisible(false);
		// Добавляю все дочерние панельки на материнскую панельку
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.NORTH;
		constraints.gridx = 1;
		constraints.fill = GridBagConstraints.NONE;
		constraints.gridheight = GridBagConstraints.REMAINDER;
		constraints.insets = new Insets(-195, 0, 0, 0);
		superPanelMain.add(superPanelLogin, constraints);
		superPanelMain.add(superPanelClients, constraints);
		superPanelMain.add(superPanelOptions, constraints);
		constraints.insets = new Insets(15, 0, 0, 0);
		superPanelMain.add(superPanelLinks, constraints);
		superPanelMain.setBackground(new Color(255, 255, 255, 32));
		linkUpdateJava.setBackground(new Color(255, 0, 0, 64));
		if(Utilities.testJavaForUpdate())
			superPanelMain.add(linkUpdateJava, constraints);
		constraints.anchor = GridBagConstraints.LAST_LINE_END;
		lnkSwitchToOtherProject.setForeground(new Color(40, 40, 200));
		superPanelMain.add(lnkSwitchToOtherProject, constraints);
		// Добавляю основную панельку и панельку для апплета на фрейм
		switchToPanel(PANELS.login);
		if(isAncestorOf(superPanelMain) == false)
			add(superPanelMain);
		setPreferredSize(new Dimension(854, 480));
		setResizable(false);
		pack();
		setLocationRelativeTo(null);
		setupListeners();
		setTooltips();
		updateOnlineMode();
	}
	public void switchToPanel(PANELS panel)
	{
		switch(panel)
		{
		case login:
			superPanelLogin.setVisible(true);
			superPanelClients.setVisible(false);
			superPanelOptions.setVisible(false);
			superPanelLinks.setVisible(true);
			panelLinks.btnDonate.setEnabled(false);
			updateOnlineMode();
			break;
		case clients:
			superPanelLogin.setVisible(false);
			superPanelClients.setVisible(true);
			superPanelOptions.setVisible(false);
			superPanelLinks.setVisible(true);
			panelLinks.btnDonate.setEnabled(true);
			boolean allowGameAutoStart = true;
			if(!panelLogin.chkAutoLogin.isEnabled())
				allowGameAutoStart = false;
			if(!panelLogin.chkAutoLogin.isSelected())
				allowGameAutoStart = false;
			if(!launcher.authentication.isAuthenticatedStrongly())
				allowGameAutoStart = false;
			if(allowGameAutoStart)
			{
				panelClients.chkAutoStartGame.setEnabled(true);
				panelClients.chkAutoStartGame.setSelected(launcher.properties.data.bAutoStartGame);
			} else {
				panelClients.chkAutoStartGame.setEnabled(false);
				panelClients.chkAutoStartGame.setSelected(false);
				launcher.properties.data.bAutoStartGame = false;
			}
			panelClients.lblName.setText(htmlText("Ваш игровой ник: <b>" + launcher.authentication.getPlayerName()+ "</b>"));
			panelClients.lblUUID.setText(htmlText("Ваш uuid: <b>" + launcher.authentication.getUUID() + "</b>"));
			break;
		case options:
			superPanelLogin.setVisible(false);
			superPanelClients.setVisible(false);
			superPanelOptions.setVisible(true);
			superPanelLinks.setVisible(true);
			break;
		}
		repaint();
		validate();
	}
	public void setBackground()
	{
		try
		{
			// Теперь фоновый рисунок задаётся здесь, а не отдельной формой с отдельным классом
			final Image background = ImageIO.read(Launcher.class.getResource("background.png"));
			final Image backgroundScaled = background.getScaledInstance(854, 480, 1);
			setContentPane(new JLabel(new ImageIcon(backgroundScaled)));
		} catch(IOException ex) {
			System.err.println(ex);
		}
	}
	private void setupListeners()
	{
		actionExecutor.setOnClose(new Runnable()
		{
			@Override
			public void run()
			{
				launcher.properties.saveToDisk();
				System.err.println("Кто-то пытается меня закрыть!");
				System.exit(0);
			}
		});
		actionExecutor.setOnLogin(new Runnable()
		{
			@Override
			public void run()
			{
				final String username = panelLogin.txtUsername.getText();
				final String password = String.copyValueOf(panelLogin.txtPassword.getPassword());
				launcher.authentication.authenticateNormal(launcher.currentProject, username, password);
			}
		});
		actionExecutor.setOnGuest(new Runnable()
		{
			@Override
			public void run()
			{
				launcher.properties.data.bAutoAuthenticate = false;
				panelLogin.chkAutoLogin.setSelected(false);
				launcher.authentication.authenticateGuest(launcher.currentProject);
			}
		});
		actionExecutor.setOnOffline(new Runnable()
		{
			@Override
			public void run()
			{
				final String username = panelLogin.txtUsername.getText();
				launcher.properties.data.bAutoAuthenticate = false;
				panelLogin.chkAutoLogin.setSelected(false);
				launcher.authentication.authenticateOffline(username);
			}
		});
		actionExecutor.setOnCheckSavePassword(new Runnable()
		{
			@Override
			public void run()
			{
				panelLogin.chkAutoLogin.setEnabled(panelLogin.chkSavePassword.isSelected());
			}
		});
		actionExecutor.setOnCheckAutoLogin(new Runnable()
		{
			@Override
			public void run()
			{
				final boolean value = panelLogin.chkAutoLogin.isSelected();
				launcher.properties.data.bAutoAuthenticate = value;
				if(!value)
				{
					panelClients.chkAutoStartGame.setSelected(false);
					launcher.properties.data.bAutoStartGame = false;
				}
				panelClients.chkAutoStartGame.setEnabled(value);
			}
		});
		actionExecutor.setOnChangeCurrentClient(new Runnable()
		{
			@Override
			public void run()
			{
				int clientId = panelClients.cbSelectClient.getSelectedIndex();
				if(clientId != -1)
					launcher.currentClient = launcher.currentProject.clients[clientId];
			}
		});
		actionExecutor.setOnPlay(new Runnable()
		{
			@Override
			public void run()
			{
				startGame();
			}
		});
		actionExecutor.setOnLogout(new Runnable()
		{
			@Override
			public void run()
			{
				launcher.authentication.logout();
				deauthenticated();
			}
		});
		actionExecutor.setOnChangeProject(new Runnable()
		{
			@Override
			public void run()
			{
				setVisible(false);
				launcher.launcherFrame = null;
				launcher.projectsFrame.setVisible(true);
				FrameLauncherMain.this.dispose();
			}
		});
		actionExecutor.setOnCheckAutoStartGame(new Runnable()
		{
			@Override
			public void run()
			{
				launcher.properties.data.bAutoStartGame = panelClients.chkAutoStartGame.isSelected();
			}
		});
		panelClients.lblName.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent arg0)
			{
				StringSelection selection = new StringSelection(launcher.authentication.getPlayerName());
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(selection, selection);
				Launcher.showGrant("Ваш никнейн скопирован в буфер обмена");
			}
		});
		panelClients.lblUUID.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent arg0)
			{
				StringSelection selection = new StringSelection(launcher.authentication.getUUID());
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(selection, selection);
				Launcher.showGrant("Ваш UUID скопирован в буфер обмена");
			}
		});
		panelOptions.btnForceUpdate.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				panelOptions.btnForceUpdate.setText("Обновление " + launcher.currentClient.captionLocalized);
				panelOptions.btnForceUpdate.setEnabled(false);
				new Thread()
				{
					@Override
					public void run()
					{
						try
						{
							launcher.resources.updateClientFiles(launcher.currentClient);
						} catch(IOException ex) {
						}
						panelOptions.btnForceUpdate.setText("Обновить клиент");
						panelOptions.btnForceUpdate.setEnabled(true);
					}
				}.start();
			}
		});
		panelOptions.lnkDirectory.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent arg0)
			{
				openLink("file://" + GlobalConfig.launcherHomeDir.getAbsolutePath());
			}
		});
		panelOptions.btnDone.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				int memory = Integer.parseInt(panelOptions.txtMemory.getText());
				if(memory != launcher.properties.data.nMemoryAllocationMB)
				{
					launcher.properties.data.nMemoryAllocationMB = memory;
					launcher.properties.saveToDisk();
					Launcher.restart(null);
				}
				deauthenticated();
			}
		});
		panelLinks.btnSetup.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				switchToPanel(PANELS.options);
			}
		});
		panelLinks.btnDonate.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				openLink(GlobalConfig.urlSiteDonate + launcher.authentication.getUUID());
			}
		});
		panelLinks.btnSite1.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				openLink(GlobalConfig.urlSiteHome1);
			}
		});
		panelLinks.btnSite2.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				openLink(GlobalConfig.urlSiteHome2);
			}
		});
		panelLinks.btnVote1.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				openLink(GlobalConfig.urlSiteVote1);
			}
		});
		panelLinks.btnVote2.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				openLink(GlobalConfig.urlSiteVote2);
			}
		});
		panelLinks.btnVote3.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				openLink(GlobalConfig.urlSiteVote3);
			}
		});
		panelLinks.btnVote4.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				openLink(GlobalConfig.urlSiteVote4);
			}
		});
		linkUpdateJava.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent arg0)
			{
				openLink("http://www.oracle.com/technetwork/java/javase/downloads/index.html");
				superPanelMain.remove(linkUpdateJava);
				linkUpdateJava.setVisible(false);
			}
		});
	}
	public void authenticated()
	{
		Launcher.showGrant(launcher.authentication.getPlayerName() + " авторизован");
		if(!launcher.getProjectClients())
		{
			switchToPanel(PANELS.login);
			return;
		}
		switchToPanel(PANELS.clients);
		// Автоматический запуск клиента
		if(launcher.checkboxDrivenStart)
		{
			if(launcher.currentClient.caption.equals(launcher.properties.data.lastStartedClient))
			{
				switch(launcher.authentication.getUserRole())
				{
				case developer:
				case administrator:
				case player:
					if(panelClients.chkAutoStartGame.isSelected())
						startGame();
					break;
				default:
					break;
				}
			}
			launcher.checkboxDrivenStart = false;
		}
	}
	public void deauthenticated()
	{
		switchToPanel(PANELS.login);
	}
	public void startGame()
	{
		panelClients.btnPlay.setEnabled(false);
		panelClients.btnLogout.setEnabled(false);
		panelLinks.btnSetup.setEnabled(false);
		launcher.gameLauncher.checkAndRunClient(launcher.currentClient);
	}
	public void gameFinished()
	{
		panelClients.btnPlay.setEnabled(true);
		panelClients.btnLogout.setEnabled(true);
		panelLinks.btnSetup.setEnabled(true);
		setTitle(GlobalConfig.createMainFrameCaption(true));
		setVisible(true);
		switchToPanel(PANELS.clients);
	}
	public void updateOnlineMode()
	{
		boolean available = !launcher.authentication.isBlocked();
		panelLogin.btnLogIn.setEnabled(available);
		panelLogin.btnGuest.setEnabled(available);
		panelLogin.btnOffline.setEnabled(available);
	}
}
