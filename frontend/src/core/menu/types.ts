export type MenuAction = 'READ' | 'CREATE' | 'UPDATE' | 'DELETE' | 'EXPORT';

export type AccessCheck = (menuCode: string, action: MenuAction) => boolean;

export type MyMenu = {
  menuCode: string;
  parentMenuCode: string | null;
  menuName: string;
  menuTypeCode: string;
  routePath: string | null;
  iconName: string | null;
  levelNo: number;
  sortOrder: number;
  actions: string[];
};

export type MenuStatus = 'idle' | 'loading' | 'ready' | 'error';

export type MenuSnapshot = {
  status: MenuStatus;
  items: MyMenu[];
};

export type SideMenuNode = {
  key: string;
  name: string;
  path?: string;
  iconName: string | null;
  children?: SideMenuNode[];
};
