export type MenuRoute = {
  menuCode: string;
  path: string;
  labelKey: string;
  primary: boolean;
};

/** 서버 routePath는 쓰지 않는다. 화면이 늘면 여기 한 줄만 추가한다. */
export const MENU_ROUTES: MenuRoute[] = [
  { menuCode: 'COMMON_CODE', path: '/mdm/common-code', labelKey: 'menu.commonCode', primary: true },
  { menuCode: 'I18N', path: '/mdm/i18n', labelKey: 'menu.i18n', primary: true },
  { menuCode: 'I18N', path: '/mdm/language', labelKey: 'menu.language', primary: false },
  { menuCode: 'COMPANY', path: '/org/company', labelKey: 'menu.org.company', primary: true },
  { menuCode: 'DEPT', path: '/org/dept', labelKey: 'menu.org.dept', primary: true },
  { menuCode: 'PERSON', path: '/org/person', labelKey: 'menu.org.person', primary: true },
  { menuCode: 'ROLE', path: '/access/role', labelKey: 'menu.access.role', primary: true },
];

export function routeForPath(pathname: string): MenuRoute | undefined {
  return MENU_ROUTES.find((item) => item.path === pathname);
}
