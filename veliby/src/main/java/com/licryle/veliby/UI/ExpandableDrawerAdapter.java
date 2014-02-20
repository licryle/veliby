package com.licryle.veliby.UI;

import com.licryle.veliby.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class ExpandableDrawerAdapter extends BaseExpandableListAdapter {
  protected LayoutInflater _mInflater;
  protected Activity _mContext;
  protected ExpandNode[] _aNavItems;

  public class ExpandNode {
    protected String _sName;
    protected ExpandNode[] _aChildren;
    protected boolean _bSelected;
    protected boolean _bForceExpand;
    protected int _iSelectMode;
    protected Object _mLinkedObj;

    public ExpandNode() {
      this._sName = "";
      this._aChildren = new ExpandNode[0];
      this._bForceExpand = false;
      this._bSelected = false;
      this._iSelectMode = 0;
      this._mLinkedObj = null;
    }

    public ExpandNode(String sName, ExpandNode[] children,
        boolean bForceExpand, int iSelectMode, Object oLinkedObject) {
      this._sName = sName;
      this._aChildren = children;
      this._bForceExpand = bForceExpand;
      this._bSelected = false;
      this._iSelectMode = iSelectMode;
      this._mLinkedObj = oLinkedObject;
    }

    public String Name() { return _sName; }
    public ExpandNode[] Children() { return _aChildren; }
    public void setChildren(ExpandNode[] aChildren) { _aChildren = aChildren; }
    public boolean ForceExpand() { return _bForceExpand; }
    public boolean isSelected() { return _bSelected; }
    public void setSelected(boolean bSelected) { _bSelected = bSelected; }
    public int getSelectedMode() { return _iSelectMode; }
    public Object getLinkedObject() { return _mLinkedObj; }
  }

  protected ExpandNode[] _loadNode(int iResId) {
    String[] aItems = _mContext.getResources().getStringArray(iResId);
    ExpandNode[] aReturnItems = new ExpandNode[aItems.length];

    for (int i = 0; i < aItems.length; i++) {
      if (aItems[i].equals("")) {
        aReturnItems[i] = new ExpandNode();
      } else {
        String[] sItems = aItems[i].split(":");

        if (sItems.length == 1) {
          aReturnItems[i] = new ExpandNode(aItems[i], new ExpandNode[0],
              false, 0, null);        
        } else {
          int iSelectMode = 0;
          if (sItems[2].equals("one_selection")) { iSelectMode = 1; } else
          if (sItems[2].equals("multi_selection")) { iSelectMode = 2; }

          int iId = _mContext.getResources().getIdentifier(sItems[0], "array",
              _mContext.getPackageName());
          aReturnItems[i] = new ExpandNode(sItems[3], _loadNode(iId),
              sItems[1].equals("force_expand"), iSelectMode, null);
        }
      }
    }

    return aReturnItems;
  }

  public ExpandableDrawerAdapter (Activity context) {
    this._mContext = context;
    this._mInflater = (LayoutInflater)
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    _aNavItems = _loadNode(R.array.menu_left_items);
  }

  public ExpandNode getChild(int groupPosition, int childPosition) {
    return _aNavItems[groupPosition].Children()[childPosition];
  }
  
  public long getChildId(int groupPosition, int childPosition) {
    return childPosition;
  }

  public View getChildView(final int groupPosition, final int childPosition,
                           boolean isLastChild, View convertView, ViewGroup parent) {
    ExpandNode mChild = getChild(groupPosition, childPosition);
    if (convertView == null) {
      if (mChild.Name().equals("")) {
        return inflateDivider();
      } else {
        convertView = inflateItem();
      }
    }

    TextView mTxt = (TextView) convertView;
    mTxt.setText(mChild.Name());

    if (mChild.isSelected()) {
      mTxt.setBackgroundColor(
          _mContext.getResources().getColor(R.color.menu_button_pressed));
      mTxt.setTextColor(Color.WHITE);
    } else {
      mTxt.setBackgroundColor(Color.TRANSPARENT);
      mTxt.setTextColor(
          _mContext.getResources().getColor(R.color.veliby_purple_light));          
    }

    return convertView;
  }

  public int getChildrenCount(int groupPosition) {
    return _aNavItems[groupPosition].Children().length;
  }

  public ExpandNode getGroup(int groupPosition) {
    return _aNavItems[groupPosition];
  }

  public void setGroup(int groupPosition, ExpandNode m) {
    _aNavItems[groupPosition] = m;
  }
  
  public int getGroupCount() {
    return _aNavItems.length;
  }
  
  public long getGroupId(int groupPosition) {
    return groupPosition;
  }

  public View getGroupView(int groupPosition, boolean isExpanded,
                           View convertView, ViewGroup parent) {
    ExpandNode mGroup = getGroup(groupPosition);
    if (mGroup.Name().equals("")) {
      return inflateDivider();
    } else if (mGroup.Children().length == 0) {
      convertView = inflateItem();
    } else {
      convertView = inflateGroup();
    }

    ((TextView) convertView).setText(mGroup.Name());

    return convertView;
  }

  public boolean hasStableIds() {
    return true;
  }
  
  public boolean isChildSelectable(int groupPosition, int childPosition) {
    return true;
  }

  protected View inflateDivider() {
    return _mContext.getLayoutInflater().inflate(
        R.layout.activity_maps_menu_divider, null);
  }

  protected View inflateItem() {
    TextView mView = (TextView) _mContext.getLayoutInflater().inflate(
        R.layout.activity_maps_menu_item, null);

    return mView;
  }

  protected View inflateGroup() {
    TextView mView = (TextView) _mContext.getLayoutInflater().inflate(
        R.layout.activity_maps_menu_group, null);

    return mView;
  }
}