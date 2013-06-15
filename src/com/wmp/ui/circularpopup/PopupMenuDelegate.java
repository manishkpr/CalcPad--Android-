package com.wmp.ui.circularpopup;

public interface PopupMenuDelegate {

	void onPopupMenuTouched(PopupMenuItem item);
	
	void onPopupMenuClosed();
	
	void onPopupMenuOpened(); 
}
