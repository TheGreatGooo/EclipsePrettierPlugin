package com.thegreatgooo.eclipse.prettier;

import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.runtime.CoreException;

public class OnSaveWatcher implements ISaveParticipant {

	@Override
	public void doneSaving(ISaveContext context) {
		// TODO Auto-generated method stub
		System.out.println("doneSaving");
	}

	@Override
	public void prepareToSave(ISaveContext context) throws CoreException {
		// TODO Auto-generated method stub
		System.out.println("prepareToSave");
		
	}

	@Override
	public void rollback(ISaveContext context) {
		// TODO Auto-generated method stub

		System.out.println("rollback");
	}

	@Override
	public void saving(ISaveContext context) throws CoreException {
		// TODO Auto-generated method stub
		System.out.println("saving");
		
	}

}
