package com.lovehunterx;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.socket.DatagramPacket;

public class GameState {
	private ConcurrentHashMap<InetSocketAddress, Client> clients;

	public GameState() {
		clients = new ConcurrentHashMap<InetSocketAddress, Client>();
	}

	public Collection<Client> getClients() {
		return clients.values();
	}

	public void addClient(Client cli) {
		clients.put(cli.getAddress(), cli);
	}

	public void removeClient(InetSocketAddress addr) {
		clients.remove(addr);
	}

	public Client getClient(InetSocketAddress addr) {
		return clients.get(addr);
	}

	public void init() {
		new Thread(new Tick()).start();
	}

	public boolean isLoggedIn(String username) {
		for (Client cli : clients.values()) {
			if (cli.getUsername().equals(username)) {
				return true;
			}
		}

		return false;
	}

	private class Tick implements Runnable {

		@Override
		public void run() {
			while (true) {
				update();
				dispatch();

				try {
					Thread.sleep((int) (1000 / 60F));
				} catch (Exception ex) {
				}
			}
		}

		public void update() {
			for (Client cli : getClients()) {
				cli.update((1000F / 60 ) / 1000);
			}
		}

		public void dispatch() {
			Iterator<Client> it = getClients().iterator();
			while (it.hasNext()) {
				Client cli = it.next();
				if (cli.isAFK()) {
					Server.disconnect(cli.getAddress());
					it.remove();
				}

				Packet delta = cli.getDeltaUpdate();
				if (delta == null) {
					continue;
				}
				
				for (Client other : getClients()) {
					if (!cli.isInRoom(other.getRoom())) {
						continue;
					}

					DatagramPacket packet = Handler.createDatagramPacket(delta, other.getAddress());
					Server.send(packet);
				}
				
				cli.clearDelta();
			}
		}

	}

}
