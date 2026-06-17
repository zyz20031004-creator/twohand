import { reactive } from "vue";

type QueryWithPage = {
  page: number;
  size: number;
  [key: string]: unknown;
};

function cloneDefaults<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

export function usePagedQuery<T extends QueryWithPage>(defaults: T) {
  const initial = cloneDefaults(defaults);
  const query = reactive(cloneDefaults(defaults)) as T;

  function resetQuery() {
    Object.assign(query, cloneDefaults(initial));
  }

  function search(run: () => void | Promise<void>) {
    query.page = 1;
    return run();
  }

  function reset(run: () => void | Promise<void>) {
    resetQuery();
    return run();
  }

  function changePage(page: number, run: () => void | Promise<void>) {
    query.page = page;
    return run();
  }

  function changeSize(size: number, run: () => void | Promise<void>) {
    query.size = size;
    query.page = 1;
    return run();
  }

  return {
    query,
    resetQuery,
    search,
    reset,
    changePage,
    changeSize,
  };
}
