package develop;

import org.apache.mesos.Protos;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;
import org.apache.mesos.Protos.ExecutorInfo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by sebastian on 10/7/14.
 */
public class FlinkMesosSched implements Scheduler {
	private ExecutorInfo executor;

	public FlinkMesosSched(ExecutorInfo executor) {
		this.executor = executor;
	}

	@Override
	public void registered(SchedulerDriver schedulerDriver, Protos.FrameworkID frameworkID, Protos.MasterInfo masterInfo) {
		System.out.println("Flink was registered: " + frameworkID.getValue() + " " + masterInfo.getHostname());
	}

	@Override
	public void reregistered(SchedulerDriver schedulerDriver, Protos.MasterInfo masterInfo) {

	}

	@Override
	public void resourceOffers(SchedulerDriver schedulerDriver, List<Protos.Offer> offers) {
		for (Protos.Offer offer : offers) {
				Protos.TaskID taskId = Protos.TaskID.newBuilder()
						.setValue("1").build();


				List<Protos.TaskInfo> tasks = new LinkedList<Protos.TaskInfo>();
				List<Protos.OfferID> offerIDs = new LinkedList<Protos.OfferID>();

			System.out.println("Launching task " + taskId.getValue());

				Protos.TaskInfo task = Protos.TaskInfo.newBuilder()
						.setName("task " + taskId.getValue())
						.setTaskId(taskId)
						.setSlaveId(offer.getSlaveId())
						.addResources(Protos.Resource.newBuilder()
								.setName("cpus")
								.setType(Protos.Value.Type.SCALAR)
								.setScalar(Protos.Value.Scalar.newBuilder().setValue(1)))
						.addResources(Protos.Resource.newBuilder()
								.setName("mem")
								.setType(Protos.Value.Type.SCALAR)
								.setScalar(Protos.Value.Scalar.newBuilder().setValue(128)))
						.setExecutor(ExecutorInfo.newBuilder(executor))
						.build();
			tasks.add(task);
			offerIDs.add(offer.getId());
			Protos.Filters filters = Protos.Filters.newBuilder().setRefuseSeconds(1).build();
			schedulerDriver.launchTasks(offerIDs, tasks);
			break;
		}
	}

	@Override
	public void offerRescinded(SchedulerDriver schedulerDriver, Protos.OfferID offerID) {

	}

	@Override
	public void statusUpdate(SchedulerDriver schedulerDriver, Protos.TaskStatus taskStatus) {

		System.out.println("Task is in state " + taskStatus.getState());
		if (taskStatus.getState() == Protos.TaskState.TASK_FINISHED) {
			schedulerDriver.stop();
		}
	}

	@Override
	public void frameworkMessage(SchedulerDriver schedulerDriver, Protos.ExecutorID executorID, Protos.SlaveID slaveID, byte[] bytes) {

	}

	@Override
	public void disconnected(SchedulerDriver schedulerDriver) {

	}

	@Override
	public void slaveLost(SchedulerDriver schedulerDriver, Protos.SlaveID slaveID) {

	}

	@Override
	public void executorLost(SchedulerDriver schedulerDriver, Protos.ExecutorID executorID, Protos.SlaveID slaveID, int i) {

	}

	@Override
	public void error(SchedulerDriver schedulerDriver, String s) {

	}
}