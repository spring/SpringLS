/*
 * Created on 2005.9.11
 */

package com.springrts.tasserver;


/**
 * @author Betalord
 */
public class StartRect {

	private boolean enabled;
	private int left;
	private int top;
	private int right;
	private int bottom;

	public StartRect() {
		enabled = false;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getLeft() {
		return left;
	}

	public void setLeft(int left) {
		this.left = left;
	}

	public int getTop() {
		return top;
	}

	public void setTop(int top) {
		this.top = top;
	}

	public int getRight() {
		return right;
	}

	public void setRight(int right) {
		this.right = right;
	}

	public int getBottom() {
		return bottom;
	}

	public void setBottom(int bottom) {
		this.bottom = bottom;
	}
}
