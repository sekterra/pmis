export default [
  {
    path: '/user',
    layout: false,
    routes: [
      {
        path: '/user/login',
        name: 'login',
        component: './user/login',
      },
      {
        path: '/user/password',
        component: './user/password',
      },
      {
        path: '/user',
        redirect: '/user/login',
      },
    ],
  },
  {
    path: '/mdm/common-code',
    name: 'commonCode',
    icon: 'table',
    hideInMenu: true,
    component: './mdm/common-code',
  },
  {
    path: '/mdm/i18n',
    name: 'i18n',
    icon: 'global',
    hideInMenu: true,
    component: './mdm/i18n',
  },
  {
    path: '/mdm/language',
    name: 'language',
    icon: 'global',
    hideInMenu: true,
    component: './mdm/language',
  },
  {
    path: '/welcome',
    name: 'welcome',
    icon: 'home',
    component: './Welcome',
  },
  {
    name: 'exception',
    icon: 'warning',
    path: '/exception',
    hideInMenu: true,
    routes: [
      {
        path: '/exception',
        redirect: '/exception/403',
      },
      {
        name: '403',
        icon: 'stop',
        path: '/exception/403',
        component: './exception/403',
      },
      {
        name: '404',
        icon: 'warning',
        path: '/exception/404',
        component: './exception/404',
      },
      {
        name: '500',
        icon: 'bug',
        path: '/exception/500',
        component: './exception/500',
      },
    ],
  },
  {
    path: '/',
    redirect: '/welcome',
  },
  {
    component: './exception/404',
    path: '/*',
  },
];
