import { MENU_ROUTES } from './menuRoutes';
import type { MyMenu, SideMenuNode } from './types';

function compareMenu(left: MyMenu, right: MyMenu): number {
  return left.levelNo - right.levelNo || left.sortOrder - right.sortOrder || left.menuCode.localeCompare(right.menuCode);
}

export function buildSideMenu(items: MyMenu[], label: (key: string) => string): SideMenuNode[] {
  const byCode = new Map(items.map((item) => [item.menuCode, item]));
  const screens = MENU_ROUTES.map((route, index) => ({
    route,
    index,
    server: byCode.get(route.menuCode),
  })).filter((row): row is typeof row & { server: MyMenu } => Boolean(row.server));
  screens.sort((left, right) => compareMenu(left.server, right.server) || left.index - right.index);

  const childrenOf = new Map<string, SideMenuNode[]>();
  const orphans: { rank: MyMenu; index: number; node: SideMenuNode }[] = [];
  for (const screen of screens) {
    const node: SideMenuNode = {
      key: screen.route.path,
      name: screen.route.primary ? screen.server.menuName : label(screen.route.labelKey),
      path: screen.route.path,
      iconName: screen.server.iconName,
    };
    const parentCode = screen.server.parentMenuCode;
    const parent = parentCode ? byCode.get(parentCode) : undefined;
    if (!parent) {
      orphans.push({ rank: screen.server, index: screen.index, node });
      continue;
    }
    const list = childrenOf.get(parent.menuCode) ?? [];
    list.push(node);
    childrenOf.set(parent.menuCode, list);
  }

  const groups = [...childrenOf.entries()].flatMap(([menuCode, children]) => {
    const server = byCode.get(menuCode);
    if (!server || children.length === 0) {
      return [];
    }
    return [
      {
        rank: server,
        index: 0,
        node: {
          key: server.menuCode,
          name: server.menuName,
          iconName: server.iconName,
          children,
        } satisfies SideMenuNode,
      },
    ];
  });

  return [...groups, ...orphans]
    .sort((left, right) => compareMenu(left.rank, right.rank) || left.index - right.index)
    .map((entry) => entry.node);
}
