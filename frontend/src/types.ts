// One API call as the request log records it. `instanceId` is whatever the service put in
// X-Instance-Id, or null if the response never reached a service (network error, or nginx
// answering by itself).
export type LogEntry = {
  id: number;
  at: string;
  method: string;
  path: string;
  status: number | null;
  instanceId: string | null;
};

export type Cart = string[];
