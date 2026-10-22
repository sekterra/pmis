import { PageContainer } from '@ant-design/pro-components';
import { useAccess, useIntl } from '@umijs/max';
import { Alert, App, Button, Input, Select, Space, Table } from 'antd';
import type { TableProps } from 'antd';
import React, { useCallback, useEffect, useState } from 'react';
import { useCodeOptions } from '@/core/codes/useCodeOptions';
import { ApiError } from '@/core/request/errors';
import type { MenuAction } from '@/core/menu/types';
import { ynParam } from '@/pages/org/shared/useYn';
import type { Company, CompanyCreateBody, CompanyUpdateBody } from '@/services/mdm/companyRecord';
import {
  createCompany,
  deleteCompany,
  getCompany,
  listCompanies,
  restoreCompany,
  updateCompany,
} from '@/services/mdm/company';
import type { SupportLanguage } from '@/services/mdm/data';
import { fetchLanguages } from '@/services/mdm/i18n';
import { listNations } from '@/services/mdm/languageAdmin';
import type { NationRecord } from '@/services/mdm/languageRecord';
import CompanyFormDrawer from './CompanyFormDrawer';
import styles from './index.module.css';
import { companyErrorText } from './rules';

type Filters = {
  keyword?: string;
  companyTypeCode?: string;
  useYn?: string;
};

function sortParams(
  sorter:
    | { field?: unknown; order?: 'ascend' | 'descend' | null }
    | Array<{ field?: unknown; order?: 'ascend' | 'descend' | null }>,
): string[] {
  const list = Array.isArray(sorter) ? sorter : [sorter];
  return list.flatMap((item) => {
    const field = typeof item.field === 'string' ? item.field : '';
    if (!field || !item.order) {
      return [];
    }
    return [`${field},${item.order === 'descend' ? 'desc' : 'asc'}`];
  });
}

const CompanyPage: React.FC = () => {
  const intl = useIntl();
  const text = (id: string, defaultMessage: string) => intl.formatMessage({ id, defaultMessage });
  const { modal } = App.useApp();
  const access = useAccess() as { can?: (menuCode: string, action: MenuAction) => boolean };
  const can = (action: MenuAction) => access.can?.('COMPANY', action) ?? false;
  const types = useCodeOptions('COMPANY_TYPE');
  const [languages, setLanguages] = useState<SupportLanguage[]>([]);
  const [nations, setNations] = useState<NationRecord[]>([]);
  const [draft, setDraft] = useState<Filters>({});
  const [filters, setFilters] = useState<Filters>({});
  const [rows, setRows] = useState<Company[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [sort, setSort] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState<string>();
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Company>();
  const [saving, setSaving] = useState(false);
  const [reload, setReload] = useState(0);

  useEffect(() => {
    let alive = true;
    fetchLanguages()
      .then((items) => {
        if (alive) {
          setLanguages(items);
        }
      })
      .catch(() => undefined);
    listNations({ page: 1, size: 200 })
      .then((result) => {
        if (alive) {
          setNations(result.items.filter((item) => item.useYn === 'Y'));
        }
      })
      .catch(() => undefined);
    return () => {
      alive = false;
    };
  }, []);

  const fail = useCallback((error: unknown) => {
    const api = error instanceof ApiError ? error : new ApiError('');
    setNotice(companyErrorText(api));
  }, []);

  useEffect(() => {
    let alive = true;
    setLoading(true);
    listCompanies({
      page,
      size: 20,
      keyword: filters.keyword,
      companyTypeCode: filters.companyTypeCode,
      useYn: ynParam(filters.useYn),
      sort,
    })
      .then((result) => {
        if (!alive) {
          return;
        }
        setRows(result.items);
        setTotal(result.total);
        setNotice(undefined);
      })
      .catch((error: unknown) => {
        if (alive) {
          fail(error);
        }
      })
      .finally(() => {
        if (alive) {
          setLoading(false);
        }
      });
    return () => {
      alive = false;
    };
  }, [page, filters, sort, reload, fail]);

  const typeName = (code: string | null) =>
    types.options.find((item) => item.value === code)?.label ?? code ?? '';

  const save = async (body: CompanyCreateBody | CompanyUpdateBody) => {
    setSaving(true);
    try {
      if (editing) {
        await updateCompany(editing.companyPk, body as CompanyUpdateBody);
      } else {
        await createCompany(body as CompanyCreateBody);
      }
      setOpen(false);
      setEditing(undefined);
      setReload((value) => value + 1);
    } finally {
      setSaving(false);
    }
  };

  const openEdit = (row: Company) => {
    getCompany(row.companyPk)
      .then((detail) => {
        setEditing(detail);
        setOpen(true);
      })
      .catch(fail);
  };

  const askDelete = (row: Company) => {
    modal.confirm({
      title: text('pages.org.company.deleteTitle', '회사를 사용 중지할까요?'),
      content: text('pages.org.company.deleteHint', '삭제하면 사용 중지됩니다. 데이터는 지워지지 않습니다.'),
      okText: text('pages.org.company.deleteOk', '삭제 확인'),
      onOk: () =>
        deleteCompany(row.companyPk)
          .then(() => setReload((value) => value + 1))
          .catch(fail),
    });
  };

  const columns: TableProps<Company>['columns'] = [
    { title: text('pages.org.company.code', '회사 코드'), dataIndex: 'companyCode', sorter: true },
    { title: text('pages.org.company.name', '회사명'), dataIndex: 'companyName', sorter: true },
    { title: text('pages.org.company.shortName', '약칭'), dataIndex: 'companyShortName' },
    {
      title: text('pages.org.company.type', '회사 유형'),
      dataIndex: 'companyTypeCode',
      render: (value: string | null) => typeName(value),
    },
    { title: text('pages.org.company.parent', '상위 회사'), dataIndex: 'parentCompanyCode' },
    { title: text('pages.org.company.nation', '국가'), dataIndex: 'nationCode' },
    { title: text('pages.org.company.sortOrder', '정렬 순서'), dataIndex: 'sortOrder', sorter: true },
    {
      title: text('pages.org.use', '사용 여부'),
      dataIndex: 'useYn',
      render: (value: string) =>
        value === 'Y' ? text('pages.org.inUse', '사용') : text('pages.org.stopped', '중지'),
    },
    {
      title: text('pages.org.actions', '작업'),
      render: (_, row) => (
        <Space>
          {row.useYn === 'Y' && can('UPDATE') ? (
            <Button size="small" onClick={() => openEdit(row)}>
              {text('pages.org.edit', '수정')}
            </Button>
          ) : null}
          {row.useYn === 'Y' && can('DELETE') ? (
            <Button size="small" onClick={() => askDelete(row)}>
              {text('pages.org.delete', '삭제')}
            </Button>
          ) : null}
          {row.useYn !== 'Y' && can('UPDATE') ? (
            <Button
              size="small"
              onClick={() =>
                restoreCompany(row.companyPk)
                  .then(() => setReload((value) => value + 1))
                  .catch(fail)
              }
            >
              {text('pages.org.restore', '복구')}
            </Button>
          ) : null}
        </Space>
      ),
    },
  ];

  return (
    <PageContainer title={text('pages.org.company.title', '회사')}>
      {notice ? <Alert type="error" showIcon title={notice} style={{ marginBottom: 16 }} /> : null}
      <Space wrap style={{ marginBottom: 16 }}>
        <Input
          allowClear
          aria-label={text('pages.org.keyword', '검색어')}
          placeholder={text('pages.org.keyword', '검색어')}
          value={draft.keyword}
          onChange={(event) => setDraft((current) => ({ ...current, keyword: event.target.value }))}
        />
        <Select
          allowClear
          aria-label={text('pages.org.company.type', '회사 유형')}
          placeholder={text('pages.org.company.type', '회사 유형')}
          style={{ minWidth: 160 }}
          options={types.options}
          value={draft.companyTypeCode}
          onChange={(value) => setDraft((current) => ({ ...current, companyTypeCode: value }))}
        />
        <Select
          allowClear
          aria-label={text('pages.org.use', '사용 여부')}
          placeholder={text('pages.org.useAll', '사용 여부 전체')}
          style={{ minWidth: 140 }}
          value={draft.useYn}
          options={[
            { value: 'Y', label: text('pages.org.inUse', '사용') },
            { value: 'N', label: text('pages.org.stopped', '중지') },
          ]}
          onChange={(value) => setDraft((current) => ({ ...current, useYn: value }))}
        />
        <Button
          type="primary"
          onClick={() => {
            setPage(1);
            setFilters(draft);
          }}
        >
          {text('pages.org.search', '조회')}
        </Button>
        <Button
          onClick={() => {
            setDraft({});
            setFilters({});
            setPage(1);
          }}
        >
          {text('pages.org.reset', '초기화')}
        </Button>
      </Space>
      <div style={{ marginBottom: 12, textAlign: 'right' }}>
        {can('CREATE') ? (
          <Button
            type="primary"
            onClick={() => {
              setEditing(undefined);
              setOpen(true);
            }}
          >
            {text('pages.org.company.create', '회사 등록')}
          </Button>
        ) : null}
      </div>
      <Table<Company>
        rowKey="companyPk"
        loading={loading}
        columns={columns}
        dataSource={rows}
        rowClassName={(row) => (row.useYn === 'Y' ? '' : styles.inactive)}
        pagination={{ current: page, pageSize: 20, total, showSizeChanger: false }}
        onChange={(pagination, _filters, sorter) => {
          setPage(pagination.current ?? 1);
          setSort(sortParams(sorter));
        }}
      />
      <CompanyFormDrawer
        open={open}
        saving={saving}
        company={editing}
        languages={languages}
        nations={nations}
        onClose={() => {
          setOpen(false);
          setEditing(undefined);
        }}
        onSubmit={save}
        onNotice={setNotice}
      />
    </PageContainer>
  );
};

export default CompanyPage;
