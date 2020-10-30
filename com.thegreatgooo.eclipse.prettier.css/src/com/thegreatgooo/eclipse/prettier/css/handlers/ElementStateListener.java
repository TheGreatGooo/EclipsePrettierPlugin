package com.thegreatgooo.eclipse.prettier.css.handlers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.ui.texteditor.IElementStateListener;
import org.eclipse.ui.texteditor.ITextEditor;

import com.thegreatgooo.eclipse.prettier.PrettierBridge;

class ElementStateListener implements IElementStateListener {
	private final ITextEditor textEditor;
	private final AtomicLong lastUndoStamp;
	private final AtomicLong formattingSaveStamp;
	private final PrettierBridge prettierBridge;

	public ElementStateListener(ITextEditor textEditor, AtomicLong lastUndoStamp, AtomicLong formattingSaveStamp,
			PrettierBridge prettierBridge) {
		this.textEditor = textEditor;
		this.lastUndoStamp = lastUndoStamp;
		this.formattingSaveStamp = formattingSaveStamp;
		this.prettierBridge = prettierBridge;
	}

	@Override
	public void elementMoved(Object originalElement, Object movedElement) {
	}

	@Override
	public void elementDeleted(Object element) {
	}

	@Override
	public void elementContentReplaced(Object element) {
	}

	@Override
	public void elementContentAboutToBeReplaced(Object element) {
	}

	@Override
	public void elementDirtyStateChanged(Object element, boolean isDirty) {
		if (!isDirty) {
			// save just happened
			Document document1 = (Document) (textEditor.getDocumentProvider()).getDocument(element);
			long undoStamp = lastUndoStamp.get();
			try {
				if (undoStamp != document1.getModificationStamp() + 1
						&& document1.getModificationStamp() != formattingSaveStamp.get()) {
					String formattedCode = prettierBridge.getFormattedCode(document1.get());
					new ReplaceEdit(0, document1.getLength(), formattedCode).apply(document1);
					formattingSaveStamp.set(document1.getModificationStamp());
					try {
						textEditor.doSave(null);
					} catch (Exception e) {
						System.out.println("error here " + e);
					}
				}
			} catch (MalformedTreeException | BadLocationException | IOException | InterruptedException
					| URISyntaxException e) {
				throw new RuntimeException("Ran into an exception while formatting", e);
			} finally {
				lastUndoStamp.set(-1);
				formattingSaveStamp.set(-1);
			}
		}
	}
}