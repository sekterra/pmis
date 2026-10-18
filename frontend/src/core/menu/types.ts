export type MenuAction = 'READ' | 'CREATE' | 'UPDATE' | 'DELETE' | 'EXPORT';

export type AccessCheck = (menuCode: string, action: MenuAction) => boolean;
