using System;
using EvoMaster.Client.Util.Extensions;
using EvoMaster.Instrumentation.Heuristic;
using EvoMaster.Instrumentation.StaticState;
using EvoMaster.Instrumentation_Shared;

namespace EvoMaster.Instrumentation.Coverage.MethodReplacement{
    public class ObjectClassReplacement{
        public static bool EqualsObject(object left, object right, string idTemplate){
            ObjectExtensions.RequireNonNull<object>(left);
            
            bool result = left.Equals(right);

            if (idTemplate == null){
                return result;
            }

            Truthness t;

            if (result){
                t = new Truthness(1d, DistanceHelper.H_NOT_NULL);
            } else{
                if (right == null){
                    t = new Truthness(DistanceHelper.H_REACHED_BUT_NULL, 1d);
                } else{
                    double baseS = DistanceHelper.H_NOT_NULL;
                    double distance = DistanceHelper.GetDistance(left, right);
                    double h = DistanceHelper.HeuristicFromScaledDistanceWithBase(baseS, distance);
                    t = new Truthness(h, 1d);
                }
            }

            ExecutionTracer.ExecutedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);

            return result;
        }
    }
}