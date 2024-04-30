import java.util.*;

public class Main {

    public static void main(String[] args) {
        new Server(0.5, 0.5, 10000).runServer();
    }

    static class Server {
        public static Random random;
        public int qMAX = 1;

        public double a;

        public double b;

        public double expectation;

        public double ts;

        public double tsmax;

        public Double t1;

        public double t2;

        public List<Task> q;

        public List<Task> inStream;

        public List<Task> outStream;

        public ServerCore core1;

        public ServerCore core2;

        public boolean type;

        public boolean k;

        public int l;

        public double nextTau;

        public double nextSigma;

        public int i;

        public double PrevT2;

        public List<ServerLog> ServerLogs;

        public Map<Long, Integer> QueueWaitTime;

        public Map<Long, Integer> busyServer;

        public Server(double ro, double expectation, double tsmax) {
            random = new Random();

            qMAX = (int) Math.round(1 / (1 - ro));
            this.expectation = expectation;

            ts = 0.0;
            this.tsmax = tsmax;
            t1 = 0.0;
            t2 = 0.0;
            q = new ArrayList<>();
            inStream = new ArrayList<>();
            outStream = new ArrayList<>();
            core1 = new ServerCore();
            core2 = new ServerCore();
            k = false;
            l = 0;
            type = false;
            nextTau = 0.0;
            nextSigma = 0.0;
            PrevT2 = 0.0;
            i = -1;
            ServerLogs = new ArrayList<>();
            QueueWaitTime = new HashMap<>();
            busyServer = new HashMap<>();
        }

        public void runServer() {
            t1 = poissonRandomInterarrivalDelay();
            double initSigma = randomGaussMaxSigma();
            nextSigma = initSigma;
            t2 = t1 + initSigma;
            ts = t1;
            core1.Task = new Task(0, t1, t1, initSigma, t2, System.currentTimeMillis());
            core1.IsBusy = true;
            inStream.add(core1.Task);

            ServerLog serverLog = new ServerLog((type ? "2" : "1"), t1, initSigma, t1, t2, (k ? "1" : "0"), l, ts, "Инициализация");
            ServerLogs.add(serverLog);
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis()   < startTime + tsmax) {
                ProcessTimeStep();
            }
        }

        public void ProcessTimeStep() {
            ServerLog serverLog = new ServerLog();
            serverLog.Type = (type ? "2" : "1");

            if (type) // Окончание обслуживания
            {
                double sigma = randomGaussMaxSigma();
                k = core1.IsBusy || core2.IsBusy;
                HandleTask(sigma);

                serverLog.Sigma = String.valueOf(sigma);
                serverLog.T2 = String.valueOf(t2);
                serverLog.Description = "Окончание обслуживания";
            } else // Приход задачи
            {
                double tau = poissonRandomInterarrivalDelay();
                t1 += tau;
                i++;

                if (i == 0) {
                    k = true;
                } else {
                    Task newTask = new Task(i, t1, nextTau, null, null, System.currentTimeMillis());
                    inStream.add(newTask);

                    if (k) {
                        if (!DelegateTaskToCore(newTask))
                            if (l <= qMAX) {
                                q.add(newTask);
                                l += 1;
                            }
                    } else {
                        core1.Task = newTask;
                        core1.IsBusy = true;
                        k = true;
                    }
                }

                nextTau = tau;

                serverLog.Tau = String.valueOf(tau);
                serverLog.T1 = String.valueOf(t1);
                serverLog.Description = "Приход задачи";
            }

            type = t1 > t2;
            ts = Math.min(t1, t2);

            serverLog.K = (k ? "1" : "0");
            serverLog.L = String.valueOf(l);
            serverLog.TS = String.valueOf(ts);

            ServerLogs.add(serverLog);
        }

        public int GetIndexOfMinPriorityTask(List<Task> queue) {
            int index = 0;
            double min = Integer.MAX_VALUE;

            for (int i = 0; i < queue.size(); i++) {
                if (Objects.nonNull(queue.get(i).Sigma) && queue.get(i).Sigma < min) {
                    min = queue.get(i).Sigma;
                    index = i;
                }
            }

            return index;
        }

        public void HandleTask(double sigma) {
            ServerCore core;
            if (core1.IsBusy)
                core = core1;
            else
                core = core2;

            core.Task.Sigma = nextSigma;
            core.Task.Delta = t2 - PrevT2;
            PrevT2 = t2;
            nextSigma = sigma;
            core.IsBusy = false;
            outStream.add(core.Task);

            if (l == 0) {
                t2 = t1 + sigma;
                core.Task = null;
            } else {
                int indexOfHighPriorityTask = GetIndexOfMinPriorityTask(q);
                core.Task = q.get(indexOfHighPriorityTask);
                q.remove(indexOfHighPriorityTask);
                l -= 1;
                long waitTime = System.currentTimeMillis() - core.Task.createTime;
                QueueWaitTime.put(waitTime, QueueWaitTime.getOrDefault(waitTime, 0) + 1);
                t2 += sigma;
            }
        }

        public boolean DelegateTaskToCore(Task newTask) {
            boolean isDelegated = false;
            ServerCore core = !core1.IsBusy ? core1 : !core2.IsBusy ? core2 : null;

            if (core != null) {
                core.Task = newTask;
                core.IsBusy = true;
                isDelegated = true;
            }

            return isDelegated;
        }


        private double poissonRandomInterarrivalDelay() {
            return Math.abs(Math.log(1.0 - Math.random()));
        }

        private double randomGaussMaxSigma() {
            return Math.max(0, Math.random() / -expectation);
        }
    }

    static class Task {
        public int I;

        public double T;

        public double Tau;

        public Double Sigma;

        public Double Delta;
        public long createTime;

        public Task()
        {
            I = 0;
            T = 0.0;
            Tau = 0.0;
            Sigma = null;
            Delta = null;
        }

        public Task(int I, Double T, Double Tau, Double Sigma, Double Delta, long createTime) {
            this.I = I;
            this.T = T;
            this.Tau = Tau;
            this.Sigma = Sigma;
            this.Delta = Delta;
            this.createTime = createTime;
        }
    }

    public static class ServerCore {
        public Task Task;

        public boolean IsBusy;

        public ServerCore() {
            Task = null;
            IsBusy = false;
        }

        public ServerCore(Task task) {
            Task = task;
            IsBusy = true;
        }
    }

    public static class ServerLog {
        public String Type = "";

        public String Tau = "";

        public String Sigma = "";

        public String T1 = "";

        public String T2 = "";

        public String K = "";

        public String L = "";

        public String TS = "";

        public String Description = "";

        public ServerLog() {
        }

        public ServerLog(String Type, Double Tau, double Sigma, Double T1, double T2, String K, int L, double TS, String Description) {
            this.Type = Type;
            this.Tau = Tau.toString();
            this.Sigma = String.valueOf(Sigma);
            this.T1 = String.valueOf(T1);
            this.T2 = String.valueOf(T2);
            this.K = K;
            this.L = String.valueOf(L);
            this.TS = String.valueOf(TS);
            this.Description = Description;
        }
    }

}