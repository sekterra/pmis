export default [
  {
    path: '/user',
    layout: false,
    routes: [
      {
        path: '/user/login',
        name: 'login',
        component: './platform/login',
      },
      {
        path: '/user/password',
        component: './platform/password',
      },
      {
        path: '/user',
        redirect: '/user/login',
      },
    ],
  },
  {
    path: '/tenants/new',
    hideInMenu: true,
    component: './platform/tenants/new',
  },
  {
    path: '/tenants/:tenantCode',
    hideInMenu: true,
    component: './platform/tenants/detail',
  },
  {
    path: '/tenants',
    name: 'tenants',
    icon: 'team',
    component: './platform/tenants',
  },
  {
    name: 'exception',
    icon: 'warning',
    path: '/exception',
    routes: [
      {
        path: '/exception',
        redirect: '/exception/403',
      },
      {
        name: '403',
        path: '/exception/403',
        component: './exception/403',
      },
      {
        name: '404',
        path: '/exception/404',
        component: './exception/404',
      },
      {
        name: '500',
        path: '/exception/500',
        component: './exception/500',
      },
    ],
  },
  {
    path: '/',
    redirect: '/tenants',
  },
  {
    component: './exception/404',
    path: '/*',
  },
];
