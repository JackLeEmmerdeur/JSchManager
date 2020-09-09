package de.jackleemmerdeur;

import com.jcraft.jsch.ChannelSftp;

public class SSHCustomLsEntrySelector
		implements ChannelSftp.LsEntrySelector
{

	String filename;
	boolean exists;

	protected SSHCustomLsEntrySelector(String filename)
	{
		this.filename = filename;
		exists = false;
	}

	@Override
	public int select(ChannelSftp.LsEntry le)
	{
		if (le.getFilename().equals(filename))
		{
			exists = true;
			return ChannelSftp.LsEntrySelector.BREAK;
		}
		return ChannelSftp.LsEntrySelector.CONTINUE;
	}

	public boolean exists()
	{
		return exists;
	}
}
